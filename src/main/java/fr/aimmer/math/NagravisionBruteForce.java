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

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class NagravisionBruteForce implements DecryptionMethod
{
    private static final int OFFSET_MAX     = 255;
    private static final int STEP_MAX       = 127;
    private static final int TOTAL_KEYS     = (OFFSET_MAX + 1) * (STEP_MAX + 1);

    /** Nombre de frames échantillonnées pour le scoring (évite les faux positifs) */
    private static final int SAMPLE_COUNT   = 5;

    /** 1 colonne sur COLUMN_STRIDE : 4x moins de calculs, précision quasi identique */
    private static final int COLUMN_STRIDE  = 4;

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
     * Lance l'attaque : échantillonne des frames, teste toutes les clés,
     * retient la meilleure selon le scoring, puis déchiffre la vidéo avec.
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
        System.out.println("[VideoScramble] BruteForce : " + totalFrames + " frames, échantillonnage en cours...");
        List<SampledFrame> sampledFrames = sampleFrames(capture, totalFrames);
        capture.release();

        if (sampledFrames.isEmpty())
            throw new RuntimeException("Impossible de lire des frames de : " + encryptedFile.getName());

        System.out.println("[VideoScramble] BruteForce : " + sampledFrames.size()
                + " frames échantillonnées, recherche de la clé...");

        int height = sampledFrames.get(0).rows.length;

        int bestOffset = 0;
        int bestStep = 0;
        double bestScore = Double.MAX_VALUE;
        int done = 0;

        // Exploration exhaustive de l'espace de clés
        for (int offset = 0; offset <= OFFSET_MAX; offset++) {
            for (int step = 0; step <= STEP_MAX; step++) {
                double frameScore = 0;
                // On somme le score sur toutes les frames échantillonnées
                for (SampledFrame sf : sampledFrames) {
                    frameScore += scoreCandidate(sf.rows, height, offset, step);
                }
                if (frameScore < bestScore) {
                    bestScore = frameScore;
                    bestOffset = offset;
                    bestStep = step;
                }
                done++;
                if (progressCallback != null) {
                    progressCallback.update(done, TOTAL_KEYS, bestOffset, bestStep, bestScore);
                }
            }
        }

        // Une fois la clé trouvée, on déchiffre vraiment la vidéo
        File outputFile = new NagravisionAlgorithm(bestOffset, bestStep).decrypt(encryptedFile, outputDir);
        System.out.println("[VideoScramble] BruteForce terminé : clé=(" + bestOffset + "," + bestStep
                + "), score=" + bestScore + ", fichier=" + outputFile.getAbsolutePath());
        return new BruteForceResult(outputFile, bestOffset, bestStep);
    }

    /**
     * Échantillonne {@value #SAMPLE_COUNT} frames réparties entre 20 % et 80 % de la vidéo
     * pour éviter les frames d'intro/outro souvent uniformes (écran noir, fondu…).
     *
     * @param capture     la capture vidéo ouverte
     * @param totalFrames nombre total de frames dans la vidéo
     * @return la liste des frames échantillonnées avec leur position
     */
    private static List<SampledFrame> sampleFrames(VideoCapture capture, int totalFrames)
    {
        List<SampledFrame> frames = new ArrayList<>();

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
            int sampled  = (rowBytes + COLUMN_STRIDE - 1) / COLUMN_STRIDE;

            if (fullRow == null || fullRow.length != rowBytes)
                fullRow = new byte[rowBytes];

            // Extraction des lignes avec sous-échantillonnage
            byte[][] rows = new byte[height][sampled];
            for (int r = 0; r < height; r++) {
                frame.get(r, 0, fullRow);
                for (int c = 0, ci = 0; c < rowBytes; c += COLUMN_STRIDE, ci++)
                    rows[r][ci] = fullRow[c];
            }
            frames.add(new SampledFrame(rows));
        }

        return frames;
    }

    /**
     * Score d'une clé candidate : somme des scores entre lignes consécutives
     * de l'image reconstituée. Plus le score est bas, mieux c'est.
     *
     * @param rows   les lignes de la frame (déjà en mémoire)
     * @param height hauteur de la frame
     * @param offset offset candidat
     * @param step   step candidat
     * @return le score total pour cette clé sur cette frame
     */
    private double scoreCandidate(byte[][] rows, int height, int offset, int step)
    {
        int[] mapping = NagravisionAlgorithm.computeRowMapping(height, offset, step);

        double total = 0;
        for (int i = 0; i < height - 1; i++) {
            total += scoring.score(rows[mapping[i]], rows[mapping[i + 1]]);
        }
        return total;
    }

    /** Une frame échantillonnée avec ses lignes. */
    private record SampledFrame(byte[][] rows) {}
}
