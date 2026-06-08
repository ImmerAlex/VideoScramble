/**
 * VideoScramble — Attaque brute force sur un chiffrement de type Nagravision.
 * <p>
 * Parcourt les 256×128 = 32 768 clés possibles, reconstitue virtuellement
 * la frame déchiffrée et utilise une {@link RowScoringFunction} pour évaluer
 * à quel point chaque candidat ressemble à une image naturelle (lignes
 * adjacentes proches). La clé minimisant la somme des scores est retenue.
 * <p>
 * Le score est moyenné sur plusieurs frames réparties dans la vidéo pour
 * éviter d'être trompé par une frame d'intro/outro ou un fond uni.
 * <p>
 * Le paradigme "exploration de clé + scoring de lissé" est partagé : seule
 * la fonction de scoring change entre les attaques (Euclide, Pearson, L1…).
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class NagravisionBruteForce implements DecryptionMethod
{
    private static final int OFFSET_MAX     = 255;
    private static final int STEP_MAX       = 127;
    private static final int KEYS_PER_ROW   = STEP_MAX + 1; // 128
    private static final int TOTAL_KEYS     = (OFFSET_MAX + 1) * KEYS_PER_ROW;

    /** Nombre de frames échantillonnées pour le scoring (évite les faux positifs) */
    private static final int SAMPLE_COUNT   = 5;

    /** Nombre cible d'echantillons par ligne apres sous-echantillonnage (stride adaptatif) */
    private static final int TARGET_SAMPLES_PER_ROW = 128;

    /** Pas de sous-echantillonnage maximal (evite de trop degrader la precision) */
    private static final int MAX_STRIDE = 16;

    private final String displayName;
    private final RowScoringFunction scoring;

    /**
     * @param displayName nom affiché dans l'UI (ex: "Euclide", "Pearson")
     * @param scoring     la fonction de scoring à utiliser
     */
    public NagravisionBruteForce(String displayName, RowScoringFunction scoring)
    {
        this.displayName = displayName;
        this.scoring = scoring;
    }

    @Override
    public String displayName()
    {
        return displayName;
    }

    @Override
    public int totalKeys()
    {
        return TOTAL_KEYS;
    }

    /**
     * Lance l'attaque : échantillonne des frames, pré-calcule la matrice
     * de scores pairwise et les mappings, puis évalue toutes les clés en
     * parallèle. La clé minimisant la somme des scores est retenue.
     *
     * @param encryptedFile    la vidéo chiffrée
     * @param outputDir        dossier de sortie
     * @param progressCallback callback de progression (peut être null)
     * @return le résultat (fichier déchiffré + clé)
     */
    @Override
    public BruteForceResult attack(File encryptedFile, File outputDir, BruteForceProgressCallback progressCallback)
    {
        System.out.println("[VideoScramble] BruteForce : input=" + encryptedFile.getAbsolutePath()
                + " (" + encryptedFile.length() + " octets)"
                + ", totalKeys=" + TOTAL_KEYS);

		VideoCapture capture = new VideoCapture(encryptedFile.getAbsolutePath());

		if (!capture.isOpened())
			throw new RuntimeException("Impossible d'ouvrir la vidéo : " + encryptedFile.getAbsolutePath());

		int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
		int width       = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);

		System.out.println("[VideoScramble] BruteForce : " + totalFrames + " frames, échantillonnage en cours...");
		List<SampledFrame> sampledFrames = sampleFrames(capture, totalFrames, width);
        capture.release();

        if (sampledFrames.isEmpty())
            throw new RuntimeException("Impossible de lire des frames de : " + encryptedFile.getName());

        System.out.println("[VideoScramble] BruteForce : " + sampledFrames.size()
                + " frames échantillonnées, recherche de la clé...");

        int height = sampledFrames.get(0).rows.length;

        // Phase 1 : pré-calcul des 32 768 mappings (ne dépend que de height)
        System.out.println("[VideoScramble] BruteForce : pré-calcul des " + TOTAL_KEYS + " mappings...");
        int[][] allMappings = precomputeMappings(height);

        // Phase 2 + 3 : pour chaque frame, pré-calcul de la matrice pairwise
        // puis évaluation parallèle de toutes les clés
        double[] cumulativeScores = new double[TOTAL_KEYS];
        AtomicInteger framesDone = new AtomicInteger(0);
        AtomicInteger bestIdx = new AtomicInteger(0);

        for (SampledFrame sf : sampledFrames)
        {
            double[][] pairwise = computePairwiseMatrix(sf.rows, height);

            double[] frameScores = new double[TOTAL_KEYS];
            IntStream.range(0, TOTAL_KEYS).parallel().forEach(idx ->
            {
                int[] mapping = allMappings[idx];
                double score = 0;
                for (int i = 0; i < height - 1; i++)
                    score += pairwise[mapping[i]][mapping[i + 1]];
                frameScores[idx] = score;
            });

            for (int i = 0; i < TOTAL_KEYS; i++)
                cumulativeScores[i] += frameScores[i];

            int done = framesDone.incrementAndGet();

            // Met à jour le meilleur score courant pour le callback
            int currentBest = bestIdx.get();
            for (int i = 0; i < TOTAL_KEYS; i++)
            {
                if (cumulativeScores[i] < cumulativeScores[currentBest])
                    currentBest = i;
            }
            bestIdx.set(currentBest);

            if (progressCallback != null)
            {
                int bestOffset = currentBest / KEYS_PER_ROW;
                int bestStep = currentBest % KEYS_PER_ROW;
                progressCallback.update(done, sampledFrames.size(), bestOffset, bestStep,
                        cumulativeScores[currentBest]);
            }
        }

        // Recherche du minimum absolu sur les scores cumulés
        int finalBestIdx = 0;
        for (int i = 1; i < TOTAL_KEYS; i++)
        {
            if (cumulativeScores[i] < cumulativeScores[finalBestIdx])
                finalBestIdx = i;
        }

        int bestOffset = finalBestIdx / KEYS_PER_ROW;
        int bestStep   = finalBestIdx % KEYS_PER_ROW;

        // Une fois la clé trouvée, on déchiffre vraiment la vidéo
        File outputFile = new NagravisionAlgorithm(bestOffset, bestStep).decrypt(encryptedFile, outputDir);
        System.out.println("[VideoScramble] BruteForce terminé : clé=(" + bestOffset + "," + bestStep
                + "), score=" + cumulativeScores[finalBestIdx] + ", fichier=" + outputFile.getAbsolutePath());
        return new BruteForceResult(outputFile, bestOffset, bestStep);
    }

    /**
     * Pré-calcule les 32 768 mappings de lignes Nagravision.
     * <p>
     * Coût : O(TOTAL_KEYS × height) ~ 23,6 M ops pour 720p — négligeable
     * car exécuté une seule fois par vidéo (height identique pour toutes les frames).
     */
    private static int[][] precomputeMappings(int height)
    {
        int[][] mappings = new int[TOTAL_KEYS][];
        for (int offset = 0; offset <= OFFSET_MAX; offset++)
        {
            for (int step = 0; step <= STEP_MAX; step++)
            {
                int idx = offset * KEYS_PER_ROW + step;
                mappings[idx] = NagravisionAlgorithm.computeRowMapping(height, offset, step);
            }
        }
        return mappings;
    }

    /**
     * Pré-calcule la matrice de scores pairwise pour une frame.
     * <p>
     * Au lieu d'appeler {@code scoring.score()} pour chaque clé × chaque paire
     * de lignes consécutives (milliards d'appels), on l'appelle une fois par
     * paire de lignes (height²/2 appels) et on stocke le résultat. L'évaluation
     * d'une clé se réduit alors à des lookups dans cette matrice.
     */
    private double[][] computePairwiseMatrix(byte[][] rows, int height)
    {
        double[][] m = new double[height][height];
        for (int i = 0; i < height; i++)
        {
            // Les paires (i,i) ne sont jamais consultées (pas de ligne consécutive identique)
            for (int j = i + 1; j < height; j++)
            {
                double s = scoring.score(rows[i], rows[j]);
                m[i][j] = s;
                m[j][i] = s;
            }
        }
        return m;
    }

    /**
     * Échantillonne {@value #SAMPLE_COUNT} frames réparties entre 20 % et 80 % de la vidéo
     * pour éviter les frames d'intro/outro souvent uniformes (écran noir, fondu…).
     *
     * @param capture     la capture vidéo ouverte
     * @param totalFrames nombre total de frames dans la vidéo
     * @return la liste des frames échantillonnées avec leur position
     */
	private static List<SampledFrame> sampleFrames(VideoCapture capture, int totalFrames, int width)
	{
		List<SampledFrame> frames = new ArrayList<>();

		// Stride adaptatif : plus la video est large, plus on sous-echantillonne
		int channels = 3; // BGR
		int stride   = computeStride(width, channels);

		// On évite les 20% du début et de la fin
		int start = Math.max(1, totalFrames / 5);
		int end = Math.max(start + 1, totalFrames * 4 / 5);
		int step = Math.max(1, (end - start) / SAMPLE_COUNT);

		Mat frame = new Mat();
		byte[] fullRow = null;
		for (int i = 0; i < SAMPLE_COUNT; i++) {
			int pos = start + i * step;
			if (pos >= totalFrames) break;

			capture.set(Videoio.CAP_PROP_POS_FRAMES, pos);
			if (!capture.read(frame) || frame.empty()) continue;

			// Vérification : certains codecs ne supportent pas le seek précis
			int actualPos = (int) capture.get(Videoio.CAP_PROP_POS_FRAMES);
			if (actualPos < pos)
			{
				capture.set(Videoio.CAP_PROP_POS_FRAMES, pos);
				if (!capture.read(frame) || frame.empty()) continue;
			}

			int height   = frame.rows();
			int rowBytes = frame.cols() * frame.channels();
			// Taille après sous-échantillonnage des colonnes
			int sampled  = (rowBytes + stride - 1) / stride;

			if (fullRow == null || fullRow.length != rowBytes)
				fullRow = new byte[rowBytes];

			// Extraction des lignes avec sous-échantillonnage
			byte[][] rows = new byte[height][sampled];
			for (int r = 0; r < height; r++) {
				frame.get(r, 0, fullRow);
				for (int c = 0, ci = 0; c < rowBytes; c += stride, ci++)
					rows[r][ci] = fullRow[c];
			}
			frames.add(new SampledFrame(rows));
		}

		return frames;
	}

    /**
     * Calcule un stride adaptatif en fonction de la resolution video.
     * <p>
     * Cible ~{@value #TARGET_SAMPLES_PER_ROW} echantillons par ligne,
     * borne entre 1 et {@value #MAX_STRIDE}. Les grandes videos sont
     * plus sous-echantillonnees (plus rapide), les petites conservent
     * plus de precision.
     *
     * @param width    largeur de la video en pixels
     * @param channels nombre de canaux (3 pour BGR)
     * @return le stride a utiliser
     */
    private static int computeStride(int width, int channels)
    {
        int rowBytes = width * channels;
        int stride   = Math.max(1, rowBytes / TARGET_SAMPLES_PER_ROW);
        return Math.min(stride, MAX_STRIDE);
    }

    /** Une frame echantillonnee avec ses lignes. */
    private record SampledFrame(byte[][] rows) {}
}
