package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.DecryptionAlgorithm;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import fr.aimmer.view.GoHomeButton;

import java.io.File;

public class EuclideSceneController implements Controller
{
	private final AppConfig config;

	public EuclideSceneController(AppConfig config)
	{
		this.config = config;
	}

	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);

		ProgressIndicator loader = new ProgressIndicator();
		root.getChildren().add(loader);

		Scene scene = new Scene(root, Main.WIDTH, Main.HEIGHT);

		Task<File> task = new Task<>()
		{
			@Override
			protected File call() throws Exception
			{
				return DecryptionAlgorithm.euclideDecrypt(config.inputFile(), config.outputDir());
			}
		};

		task.setOnSucceeded(event -> {
			root.getChildren().clear();
			root.getChildren().addAll(
					new Label("Déchiffrement terminé : " + task.getValue().getName()),
					new GoHomeButton()
			);
		});

		task.setOnFailed(event -> {
			root.getChildren().clear();
			root.getChildren().addAll(
					new Label("Erreur : " + task.getException().getMessage()),
					new GoHomeButton()
			);
		});

		new Thread(task).start();

		return scene;
	}
}
