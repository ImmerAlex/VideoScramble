package fr.aimmer.math;

import fr.aimmer.utils.MathUtils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DecryptionAlgorithm
{
	public static File euclideDecrypt(File encryptedFile, File outputDir)
	{
		VideoCapture capture = new VideoCapture(encryptedFile.getAbsolutePath());

		Mat firstFrame = new Mat();
		//TODO: comprendre pourquoi read() retourne false sur la vidéo chiffrée
		if (!capture.read(firstFrame)) {
			throw new RuntimeException("Impossible d'obtenir la première frame de la vidéo");
		}

		List<Double> distances = new ArrayList<>(firstFrame.rows() - 1);
		for (int i = 0; i < firstFrame.rows() - 1; i++) {
			Mat current = firstFrame.row(i);
			Mat next    = firstFrame.row(i + 1);
			distances.add(MathUtils.euclideanDistance(current, next));
		}

		throw new UnsupportedOperationException("Méthode de déchiffrage par Euclide en cours d'implémentation");
	}
}
