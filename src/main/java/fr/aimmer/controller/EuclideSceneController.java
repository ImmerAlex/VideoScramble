package fr.aimmer.controller;

import fr.aimmer.Main;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

public class EuclideSceneController implements Controller
{
	@Override
	public Scene get()
	{
		VBox root = new VBox();
		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}
}
