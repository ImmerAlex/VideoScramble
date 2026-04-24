package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;

public class PearsonSceneController implements Controller
{
	private final AppConfig config;

	public PearsonSceneController(AppConfig config)
	{
		this.config = config;
	}

	@Override
	public Scene get()
	{
		VBox root = new VBox();
		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}
}
