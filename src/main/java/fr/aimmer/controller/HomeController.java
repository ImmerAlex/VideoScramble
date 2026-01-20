package fr.aimmer.controller;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import static fr.aimmer.App.HEIGHT;
import static fr.aimmer.App.WIDTH;

public class HomeController implements Controller
{
	private final Button button;

	public HomeController()
	{
		this.button = new Button("Go to Settings");
		this.button.setOnAction(_ -> SceneManager.getInstance().switchTo("settings", true));
	}

	@Override
	public Scene get()
	{
		return new Scene(button, WIDTH, HEIGHT);
	}
}
