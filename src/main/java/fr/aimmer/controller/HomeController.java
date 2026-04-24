package fr.aimmer.controller;

import fr.aimmer.ui.scene.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import static fr.aimmer.Main.HEIGHT;
import static fr.aimmer.Main.WIDTH;

public class HomeController implements Controller
{
	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);

		Button encryptButton = new Button("Vidéo encryptée");
		encryptButton.setOnAction(_ -> SceneManager.getInstance().switchTo("scene:1"));
		encryptButton.setPadding(new Insets(10, 40, 10, 40));

		Button euclideButton = new Button("Décryptage Euclide");
		euclideButton.setOnAction(_ -> SceneManager.getInstance().switchTo("scene:euclide"));
		euclideButton.setPadding(new Insets(10, 40, 10, 40));

		Button pearsonButton = new Button("Décryptage Pearson");
		pearsonButton.setOnAction(_ -> SceneManager.getInstance().switchTo("scene:pearson"));
		pearsonButton.setPadding(new Insets(10, 40, 10, 40));

		root.getChildren().addAll(encryptButton, euclideButton, pearsonButton);

		return new Scene(root, WIDTH, HEIGHT);
	}
}
