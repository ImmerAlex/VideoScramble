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

		Button button = new Button("Vidéo encryptée");
		button.setOnAction(_ -> SceneManager.getInstance().switchTo("scene:1"));
		button.setPadding(new Insets(10, 40, 10, 40));
		root.getChildren().add(button);

		button = new Button("Decryptage Euclide");
		button.setOnAction(_ -> SceneManager.getInstance().switchTo("scene:euclide"));
		button.setPadding(new Insets(10, 40, 10, 40));
		root.getChildren().add(button);

		button = new Button("Decryptage Pearson");
		button.setOnAction(_ -> SceneManager.getInstance().switchTo("scene:pearson"));
		button.setPadding(new Insets(10, 40, 10, 40));
		root.getChildren().add(button);


		return new Scene(root, WIDTH, HEIGHT);
	}
}
