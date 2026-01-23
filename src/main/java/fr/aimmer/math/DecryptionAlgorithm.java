package fr.aimmer.math;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import fr.aimmer.utils.MathUtils;

public class DecryptionAlgorithm {

	public static File euclideDecrypt(File encryptedFile) {
		File decryptedFile = null;

		// Charger la vidéo
		VideoCapture encryptedVideoCapture = new VideoCapture(encryptedFile.getAbsolutePath());

		// Récupérer la première frame de la vidéo
		Mat firstEncryptedVideoFrame = new Mat();
		//TODO: Comprendre pourquoi il ne récupère pas la première frame
		// ou pourquoi cette méthode renvoi false ?
		if (encryptedVideoCapture.read(firstEncryptedVideoFrame) == false) {
			throw new RuntimeException("Impossible d'obtenir la première frame de la vidéo");
		}

		List<Double> distances = new ArrayList<>(firstEncryptedVideoFrame.rows() - 1);
		for (int i = 0; i < firstEncryptedVideoFrame.rows() - 1; i++) {
			Mat currentLine = firstEncryptedVideoFrame.row(i);
			Mat nextLine = firstEncryptedVideoFrame.row(i+1);
			distances.add(MathUtils.euclideanDistance(currentLine, nextLine));
		}

		throw new UnsupportedOperationException("Methode de déchiffrage par Euclide en cours d'implémentation");
	}

}
