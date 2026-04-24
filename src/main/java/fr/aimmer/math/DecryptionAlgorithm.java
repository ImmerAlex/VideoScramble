package fr.aimmer.math;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.function.IntConsumer;

public class DecryptionAlgorithm
{
    /**
     * Casse la clé par force brute (distance euclidienne) puis déchiffre la vidéo.
     * <p>
     * Pour chacune des 256×128 = 32 768 clés possibles, on reconstitue virtuellement
     * la frame déchiffrée et on calcule la somme des distances euclidiennes entre
     * lignes consécutives. Une image naturelle a des lignes proches, donc la clé
     * ayant le score minimal est la meilleure candidate.
     *
     * @param progressCallback appelé avec le nombre de clés testées (0..32768), peut être null
     */
    public static BruteForceResult euclideDecrypt(File encryptedFile, File outputDir, IntConsumer progressCallback)
    {
        VideoCapture capture = new VideoCapture(encryptedFile.getAbsolutePath());

        if (!capture.isOpened())
            throw new RuntimeException("Impossible d'ouvrir la vidéo : " + encryptedFile.getAbsolutePath());

        Mat firstFrame = new Mat();
        if (!capture.read(firstFrame)) {
            capture.release();
            throw new RuntimeException("Impossible de lire la première frame de : " + encryptedFile.getName());
        }
        capture.release();

        int height = firstFrame.rows();
        int cols = firstFrame.cols();
        int channels = firstFrame.channels();

        // Pré-extraction en tableaux Java : une seule JNI call par ligne,
        // au lieu d'une call par pixel dans la boucle de scoring.
        byte[][] rows = new byte[height][cols * channels];
        for (int r = 0; r < height; r++) {
            firstFrame.get(r, 0, rows[r]);
        }

        int bestOffset = 0;
        int bestStep = 0;
        double bestScore = Double.MAX_VALUE;
        int done = 0;

        for (int offset = 0; offset <= 255; offset++) {
            for (int step = 0; step <= 127; step++) {
                double score = scoreEuclidean(rows, height, offset, step);
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

    /**
     * Score d'une clé candidate : somme des distances euclidiennes entre lignes
     * consécutives de l'image reconstituée. Plus le score est bas, mieux c'est.
     * <p>
     * Comme mapping[i] = j signifie "la ligne i de l'original est à la ligne j
     * du chiffré" (encrypted[mapping[i]] = original[i]), on lit directement
     * rows[mapping[i]] pour obtenir la ligne i de l'image déchiffrée candidate.
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
