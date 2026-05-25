/**
 * VideoScramble — Scène de résultat de chiffrement.
 * <p>
 * Affiche côte à côte la vidéo originale et la vidéo chiffrée (ou déchiffrée
 * selon le mode). Le traitement OpenCV est lancé dans un {@link javafx.concurrent.Task}
 * pour ne pas bloquer le thread JavaFX. Un spinner de chargement est affiché
 * pendant le traitement.
 * <p>
 * Cette scène est enregistrée dynamiquement par {@link VideoSelectionController}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.EncryptionMethod;
import fr.aimmer.utils.MediaViewFactory;
import fr.aimmer.view.GoHomeButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;

public class EncryptionSceneController implements Controller
{
	private final AppConfig config;
	private final EncryptionMethod algo;

	/**
	 * @param config configuration de session
	 * @param algo   l'algorithme de chiffrement sélectionné
	 */
	public EncryptionSceneController(AppConfig config, EncryptionMethod algo)
	{
		this.config = config;
		this.algo = algo;
	}

	/**
	 * Construit la scène de traitement et résultat.
	 * <p>
	 * Affiche d'abord un spinner, puis lance le chiffrement dans un thread
	 * séparé. Une fois terminé, remplace le spinner par les deux vidéos.
	 *
	 * @return la scène de chiffrement
	 */
	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);

		// Spinner en attendant la fin du traitement
		ProgressIndicator loader = new ProgressIndicator();
		root.getChildren().add(loader);

		Scene scene = new Scene(root, Main.WIDTH, Main.HEIGHT);

		// Tâche de fond : chiffrement OpenCV
		Task<List<File>> task = new Task<>()
		{
			@Override
			protected List<File> call() throws Exception
			{
				System.out.println("[VideoScramble] Début chiffrement : algo=" + algo.displayName()
						+ ", input=" + config.inputFile().getAbsolutePath()
						+ ", outputDir=" + config.outputDir().getAbsolutePath());

				// On délègue à l'algo (encrypt ou decrypt selon le mode)
				File processedFile = config.mode() == 'C'
						? algo.encrypt(config.inputFile(), config.outputDir())
						: algo.decrypt(config.inputFile(), config.outputDir());

				System.out.println("[VideoScramble] Chiffrement terminé : " + processedFile.getAbsolutePath()
						+ " (" + processedFile.length() + " octets)");

				return List.of(config.inputFile(), processedFile);
			}
		};

		// Succès : on affiche les deux vidéos côte à côte
		task.setOnSucceeded(event -> {
			List<File> files = task.getValue();
			root.getChildren().clear();

			MediaView originalView  = MediaViewFactory.getMediaView(files.get(0));
			MediaView processedView = MediaViewFactory.getMediaView(files.get(1));

			// Infos sur l'algo et la clé
			Label algoLabel = new Label("Algorithme : " + algo.displayName());
			algoLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

			Label offsetLabel = new Label("OFFSET: " + config.offset());
			offsetLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
			Label stepLabel = new Label("STEP: " + config.step());
			stepLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

			HBox topLabels = new HBox(20, offsetLabel, stepLabel);
			topLabels.setAlignment(Pos.CENTER);
			root.getChildren().addAll(algoLabel, topLabels);

			// Les deux vidéos côte à côte
			HBox videoBox = new HBox(20, originalView, processedView);
			videoBox.setAlignment(Pos.CENTER);
			root.getChildren().add(videoBox);

			// Binding pour que les vidéos s'adaptent à la largeur de la fenêtre
			originalView.fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
			processedView.fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
			originalView.setPreserveRatio(true);
			processedView.setPreserveRatio(true);

			GoHomeButton goHomeButton = new GoHomeButton();
			root.getChildren().add(goHomeButton);
			VBox.setMargin(goHomeButton, new Insets(20, 0, 0, 0));
		});

		// Échec : on affiche l'erreur
		task.setOnFailed(event -> {
			Throwable ex = task.getException();
			System.err.println("[VideoScramble] ERREUR chiffrement : " + ex.getMessage());
			ex.printStackTrace(System.err);
			root.getChildren().clear();
			root.getChildren().add(new Label("Erreur : " + ex.getMessage()));
		});

		new Thread(task).start();

		return scene;
	}
}
