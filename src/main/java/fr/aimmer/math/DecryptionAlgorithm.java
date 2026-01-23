package fr.aimmer.math;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class DecryptionAlgorithm {

	public static File euclideDecrypt(File encryptedFile) throws IOException {
		File decryptedFile = null;

		// Charger la vidéo
		VideoCapture encryptedVideoCapture = new VideoCapture(encryptedFile.getAbsolutePath());

		// Récupérer la première frame de la vidéo
		Mat firstEncryptedVideoFrame = new Mat();
		if (!encryptedVideoCapture.read(firstEncryptedVideoFrame)) {
			throw new RuntimeException("Impossible d'obtenir la première frame de la vidéo");
		}

		List<Double> distances = new ArrayList<>(firstEncryptedVideoFrame.rows() - 1);
		for (int i = 0; i < firstEncryptedVideoFrame.rows() - 1; i++) {
			Mat currentLine = firstEncryptedVideoFrame.row(i);
			Mat nextLine = firstEncryptedVideoFrame.row(i+1);
			// distances.add();
		}

		throw new UnsupportedOperationException("Methode de déchiffrage par Euclide en cours d'implémentation");
	}

}
