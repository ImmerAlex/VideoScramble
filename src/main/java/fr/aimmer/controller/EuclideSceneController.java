package fr.aimmer.controller;

import fr.aimmer.Main;
import fr.aimmer.math.DecryptionAlgorithm;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

public class EuclideSceneController implements Controller
{
	@Override
	public Scene get()
	{

		DecryptionAlgorithm.euclideDecrypt(Main.FILE);

		VBox root = new VBox();
		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}
}
