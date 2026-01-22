package fr.aimmer.math;

import fr.aimmer.Main;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;

import static fr.aimmer.utils.MathUtils.largestPowerOfTwo;

public class EncryptionAlgorithm
{
	/**
	 * Encrypt la vidéo style 'canal+' et retourne l'URL de la nouvelle vidéo
	 *
	 * @param file Chemin vers la vidéo à encrypter
	 *
	 * @return File - La nouvelle vidéo encrypter
	 */
	public static File encrypt(File file)
	{
		VideoCapture capture = new VideoCapture(file.getAbsolutePath());

		File outputFile = new File(Main.OUTPUT_DIR, ( Main.MODE == 'C' ? "encrypted_" : "decrypted_" ) + file.getName());
		int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
		int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		int fps = (int) capture.get(Videoio.CAP_PROP_FPS);

		VideoWriter writer = new VideoWriter(
				outputFile.getAbsolutePath(),
				VideoWriter.fourcc('m', 'p', '4', 'v'),
				fps,
				new Size(width, height)
		);

		int[] rowMapping = computeRowMapping(height, Main.OFFSET, Main.STEP);

		Mat frame = new Mat();
		Mat encrypted = new Mat();

		while (capture.read(frame)) {
			applyRowPermutation(frame, encrypted, rowMapping);
			writer.write(encrypted);
		}

		capture.release();
		writer.release();

		return outputFile;
	}

	private static int[] computeRowMapping(int height, int offset, int step)
	{
		int[] mapping = new int[height];
		int base = 0;
		int remaining = height;
		int destIndex = 0;

		while (remaining > 1) {
			int blockSize = largestPowerOfTwo(remaining);

			for (int i = 0; i < blockSize; i++) {
				int dst = base + ( ( offset + ( 2 * step + 1 ) * i ) % blockSize );
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

	private static void applyRowPermutation(Mat source, Mat dest, int[] mapping)
	{
		source.copyTo(dest);

		for (int i = 0; i < mapping.length; i++) {
			source.row(i).copyTo(dest.row(mapping[i]));
		}
	}
}
