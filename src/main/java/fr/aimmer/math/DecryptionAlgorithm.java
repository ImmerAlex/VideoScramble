package fr.aimmer.math;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class DecryptionAlgorithm
{
    private static final int SAMPLE_COUNT   = 5;
    // 1 colonne sur COLUMN_STRIDE : 4× moins de calculs par paire de lignes,
    // sans perte significative de précision pour la comparaison de lissé.
    private static final int COLUMN_STRIDE  = 4;

    /**
     * Casse la clé par force brute (distance euclidienne) puis déchiffre la vidéo.
     * <p>
     * Pour chacune des 256×128 = 32 768 clés possibles, on reconstitue virtuellement
     * la frame déchiffrée et on calcule la somme des distances euclidiennes entre
     * lignes consécutives. Une image naturelle a des lignes proches, donc la clé
     * ayant le score minimal est la meilleure candidate.
     * <p>
     * Le score est moyenné sur plusieurs frames réparties dans la vidéo pour éviter
     * d'être trompé par une frame d'intro/outro ou un fond uni.
     *
     * @param progressCallback appelé avec le nombre de clés testées (0..32768), peut être null
     */
    public static BruteForceResult euclideDecrypt(File encryptedFile, File outputDir, IntConsumer progressCallback)
    {
        VideoCapture capture = new VideoCapture(encryptedFile.getAbsolutePath());

        if (!capture.isOpened())
            throw new RuntimeException("Impossible d'ouvrir la vidéo : " + encryptedFile.getAbsolutePath());

        int totalFrames = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        List<byte[][]> sampledFrames = sampleFrames(capture, totalFrames);
        capture.release();

        if (sampledFrames.isEmpty())
            throw new RuntimeException("Impossible de lire des frames de : " + encryptedFile.getName());

        int height = sampledFrames.get(0).length;

        int bestOffset = 0;
        int bestStep = 0;
        double bestScore = Double.MAX_VALUE;
        int done = 0;

        for (int offset = 0; offset <= 255; offset++) {
            for (int step = 0; step <= 127; step++) {
                double score = 0;
                for (byte[][] rows : sampledFrames) {
                    score += scoreEuclidean(rows, height, offset, step);
                }
                if (score < bestScore) {
                    bestScore = score;
                    bestOffset = offset;
                    bestStep = step;
                }
                done++;
                if (progressCallback != null && done % 512 == 0) {
                    progressCallback.accept(done);
                }
            }
        }

        File outputFile = EncryptionAlgorithm.decrypt(encryptedFile, outputDir, bestOffset, bestStep);
        return new BruteForceResult(outputFile, bestOffset, bestStep);
    }

    // Échantillonne SAMPLE_COUNT frames réparties entre 20 % et 80 % de la vidéo
    // pour éviter les frames d'intro/outro souvent uniformes.
    private static List<byte[][]> sampleFrames(VideoCapture capture, int totalFrames)
    {
        List<byte[][]> frames = new ArrayList<>();

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
            frames.add(rows);
        }

        return frames;
    }

    /**
     * Score d'une clé candidate : somme des distances euclidiennes entre lignes
     * consécutives de l'image reconstituée. Plus le score est bas, mieux c'est.
     */
    private static double scoreEuclidean(byte[][] rows, int height, int offset, int step)
    {
        int[] mapping = EncryptionAlgorithm.computeRowMapping(height, offset, step);

        double total = 0;
        for (int i = 0; i < height - 1; i++) {
            total += rowDistance(rows[mapping[i]], rows[mapping[i + 1]]);
        }
        return total;
    }

    private static double rowDistance(byte[] row1, byte[] row2)
    {
        double sum = 0;
        for (int i = 0; i < row1.length; i++) {
            double diff = (row1[i] & 0xFF) - (row2[i] & 0xFF);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public record BruteForceResult(File outputFile, int offset, int step)
    {
    }
}
