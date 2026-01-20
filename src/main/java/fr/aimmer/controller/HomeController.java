package fr.aimmer.controller;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import static fr.aimmer.Main.HEIGHT;
import static fr.aimmer.Main.WIDTH;

public class HomeController implements Controller
{
	@Override
	public Scene get()
	{
		Button button = new Button("SCENE 1");
		button.setOnAction(_ ->
								   SceneManager.getInstance().switchTo("scene:1"));

		return new Scene(button, WIDTH, HEIGHT);
	}
}
