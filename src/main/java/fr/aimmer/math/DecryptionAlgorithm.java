package fr.aimmer.math;

import java.io.File;
import java.io.IOException;

import org.opencv.videoio.VideoCapture;

public class DecryptionAlgorithm {

	public static File euclideDecrypt(File encryptedFile) throws IOException {
		File decryptedFile = null;
		// Charger la vidéo
		var encryptedVideoCapture = new VideoCapture(encryptedFile.getAbsolutePath());
		// TODO: Tout faire en fait

		return decryptedFile;
	}

	public static File pearsonDecrypt(File file) {
		return file;
	}
}
