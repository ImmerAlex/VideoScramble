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

		Button encryptButton = new Button("Encryption");
		encryptButton.setOnAction(e -> SceneManager.getInstance().switchTo("scene:encryption:selection"));
		encryptButton.setPadding(new Insets(10, 40, 10, 40));

		Button decryptButton = new Button("Décryption");
		decryptButton.setOnAction(e -> SceneManager.getInstance().switchTo("scene:decryption:selection"));
		decryptButton.setPadding(new Insets(10, 40, 10, 40));

		root.getChildren().addAll(encryptButton, decryptButton);

		return new Scene(root, WIDTH, HEIGHT);
	}
}
