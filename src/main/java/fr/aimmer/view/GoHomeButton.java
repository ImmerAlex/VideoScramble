package fr.aimmer.view;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.control.Button;

public class GoHomeButton extends Button
{
	public GoHomeButton() {
		super("Home");
		this.setOnAction(_ -> {
			SceneManager.getInstance().switchTo("home", true);
		});
	}
}
