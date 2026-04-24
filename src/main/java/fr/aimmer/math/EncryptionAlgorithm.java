package fr.aimmer.math;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;

import static fr.aimmer.utils.MathUtils.largestPowerOfTwo;

public class EncryptionAlgorithm
{
    public static File encrypt(File inputFile, File outputDir, int offset, int step)
    {
        return process(inputFile, new File(outputDir, "generated/crypted"), "encrypted_", offset, step, false);
    }

    public static File decrypt(File inputFile, File outputDir, int offset, int step)
    {
        return process(inputFile, new File(outputDir, "generated/decrypted"), "decrypted_", offset, step, true);
    }

    private static File process(File inputFile, File outputDir, String prefix, int offset, int step, boolean inverse)
    {
        outputDir.mkdirs();

        VideoCapture capture = new VideoCapture(inputFile.getAbsolutePath());

        File outputFile = new File(outputDir, prefix + inputFile.getName());
        int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        int fps = (int) capture.get(Videoio.CAP_PROP_FPS);

        VideoWriter writer = new VideoWriter(
                outputFile.getAbsolutePath(),
                VideoWriter.fourcc('m', 'p', '4', 'v'),
                fps,
                new Size(width, height)
        );

        int[] rowMapping = computeRowMapping(height, offset, step);

        Mat frame = new Mat();
        Mat permuted = new Mat();

        while (capture.read(frame)) {
            if (inverse)
                applyInverseRowPermutation(frame, permuted, rowMapping);
            else
                applyRowPermutation(frame, permuted, rowMapping);
            writer.write(permuted);
        }

        capture.release();
        writer.release();

        return outputFile;
    }

    // package-private pour les tests unitaires
    static int[] computeRowMapping(int height, int offset, int step)
    {
        int[] mapping = new int[height];
        int base = 0;
        int remaining = height;
        int destIndex = 0;

        while (remaining > 1) {
            int blockSize = largestPowerOfTwo(remaining);

            for (int i = 0; i < blockSize; i++) {
                int dst = base + ((offset + (2 * step + 1) * i) % blockSize);
                mapping[destIndex++] = dst;
            }

            base += blockSize;
            remaining -= blockSize;
        }

        if (remaining == 1) {
            mapping[destIndex] = base;
        }

        return mapping;
    }

    // dest[mapping[i]] = source[i]
    private static void applyRowPermutation(Mat source, Mat dest, int[] mapping)
    {
        source.copyTo(dest);
        for (int i = 0; i < mapping.length; i++) {
            source.row(i).copyTo(dest.row(mapping[i]));
        }
    }

    // dest[i] = source[mapping[i]]  — opération inverse, utilisée pour le déchiffrement
    private static void applyInverseRowPermutation(Mat source, Mat dest, int[] mapping)
    {
        source.copyTo(dest);
        for (int i = 0; i < mapping.length; i++) {
            source.row(mapping[i]).copyTo(dest.row(i));
        }
    }
}
