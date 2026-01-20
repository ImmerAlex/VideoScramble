package fr.aimmer.controller;

import fr.aimmer.view.GoHomeButton;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import static fr.aimmer.App.HEIGHT;
import static fr.aimmer.App.WIDTH;

public class SettingsController implements Controller
{
	private final VBox root = new VBox();

	public SettingsController()
	{
		root.setSpacing(20);
		root.getChildren().addAll(
				new Label("Settings page"),
				new GoHomeButton()
		);
	}

	@Override
	public Scene get()
	{
		return new Scene(root, WIDTH, HEIGHT);
	}
}
