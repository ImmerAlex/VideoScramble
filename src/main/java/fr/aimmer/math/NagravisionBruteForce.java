package fr.aimmer.math;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Attaque brute force sur un chiffrement de type Nagravision.
 * <p>
 * Parcourt les 256×128 = 32 768 clés possibles, reconstitue virtuellement
 * la frame déchiffrée et utilise une {@link RowScoringFunction} pour évaluer
 * à quel point chaque candidat ressemble à une image naturelle (lignes
 * adjacentes proches). La clé minimisant la somme des scores est retenue.
 * <p>
 * Le score est moyenné sur plusieurs frames réparties dans la vidéo pour
 * éviter d'être trompé par une frame d'intro/outro ou un fond uni.
 * <p>
 * Tient compte de la variation d'offset par frame introduite dans
 * {@link NagravisionAlgorithm} ({@code effectiveOffset = (offset + frameIndex) & 0xFF}).
 * <p>
 * Le paradigme "exploration de clé + scoring de lissé" est partagé : seule
 * la fonction de scoring change entre les attaques (Euclide, Pearson, L1…).
 */
public class NagravisionBruteForce implements DecryptionMethod
{
    private static final int OFFSET_MAX     = 255;
    private static final int STEP_MAX       = 127;
    private static final int TOTAL_KEYS     = (OFFSET_MAX + 1) * (STEP_MAX + 1);
    private static final int SAMPLE_COUNT   = 5;
    // 1 colonne sur COLUMN_STRIDE : 4× moins de calculs par paire de lignes,
    // sans perte significative de précision pour la comparaison de lissé.
    private static final int COLUMN_STRIDE  = 4;

    private final String displayName;
    private final RowScoringFunction scoring;

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

    @Override
    public BruteForceResult attack(File encryptedFile, File outputDir, IntConsumer progressCallback)
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

        for (int offset = 0; offset <= OFFSET_MAX; offset++) {
            for (int step = 0; step <= STEP_MAX; step++) {
                double frameScore = 0;
                for (SampledFrame sf : sampledFrames) {
                    frameScore += scoreCandidate(sf.rows, height, offset, step, sf.frameIndex);
                }
                if (frameScore < bestScore) {
                    bestScore = frameScore;
                    bestOffset = offset;
                    bestStep = step;
                }
                done++;
                if (progressCallback != null && done % 512 == 0) {
                    progressCallback.accept(done);
                }
            }
        }

        File outputFile = new NagravisionAlgorithm(bestOffset, bestStep).decrypt(encryptedFile, outputDir);
        System.out.println("[VideoScramble] BruteForce terminé : clé=(" + bestOffset + "," + bestStep
                + "), score=" + bestScore + ", fichier=" + outputFile.getAbsolutePath());
        return new BruteForceResult(outputFile, bestOffset, bestStep);
    }

    /**
     * Échantillonne SAMPLE_COUNT frames réparties entre 20 % et 80 % de la vidéo
     * pour éviter les frames d'intro/outro souvent uniformes.
     * Retourne la position de chaque frame pour la variation d'offset par frame.
     */
    private static List<SampledFrame> sampleFrames(VideoCapture capture, int totalFrames)
    {
        List<SampledFrame> frames = new ArrayList<>();

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

            int actualPos = (int) capture.get(Videoio.CAP_PROP_POS_FRAMES);
            if (actualPos < pos)
            {
                capture.set(Videoio.CAP_PROP_POS_FRAMES, pos);
                if (!capture.read(frame) || frame.empty()) continue;
            }

            int height   = frame.rows();
            int rowBytes = frame.cols() * frame.channels();
            int sampled  = (rowBytes + COLUMN_STRIDE - 1) / COLUMN_STRIDE;

            if (fullRow == null || fullRow.length != rowBytes)
                fullRow = new byte[rowBytes];

            byte[][] rows = new byte[height][sampled];
            for (int r = 0; r < height; r++) {
                frame.get(r, 0, fullRow);
                for (int c = 0, ci = 0; c < rowBytes; c += COLUMN_STRIDE, ci++)
                    rows[r][ci] = fullRow[c];
            }
            frames.add(new SampledFrame(pos, rows));
        }

        return frames;
    }

    /**
     * Score d'une clé candidate : somme des scores de la fonction injectée
     * entre lignes consécutives de l'image reconstituée. Plus le score est bas,
     * mieux c'est.
     * <p>
     * L'offset effectif inclut la position de la frame dans la vidéo pour
     * correspondre à la variation par frame de {@link NagravisionAlgorithm}.
     */
    private double scoreCandidate(byte[][] rows, int height, int offset, int step, int frameIndex)
    {
        int effectiveOffset = (offset + frameIndex) & 0xFF;
        int[] mapping = NagravisionAlgorithm.computeRowMapping(height, effectiveOffset, step);

        double total = 0;
        for (int i = 0; i < height - 1; i++) {
            total += scoring.score(rows[mapping[i]], rows[mapping[i + 1]]);
        }
        return total;
    }

    private record SampledFrame(int frameIndex, byte[][] rows) {}
}
