/**
 * VideoScramble — Écran de sélection du fichier vidéo à chiffrer.
 * <p>
 * Propose un explorateur de fichiers (FileChooser) pour choisir la vidéo source.
 * Le fichier retenu est passé au controller de chiffrement suivant.
 * <p>
 * Cette scène est enregistrée dynamiquement par {@link EncryptionSelectionController}
 * car elle dépend de l'algorithme choisi.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.EncryptionMethod;
import fr.aimmer.ui.scene.SceneManager;
import fr.aimmer.view.GoHomeButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Function;

public class VideoSelectionController implements Controller
{
	private final AppConfig config;
	private final String algoLabel;
	private final Function<AppConfig, EncryptionMethod> algoFactory;

	/**
	 * @param config      la config de session
	 * @param algoLabel   nom de l'algo pour l'affichage (ex: "Nagravision")
	 * @param algoFactory fabrique d'{@link EncryptionMethod} à partir de la config
	 */
	public VideoSelectionController(AppConfig config, String algoLabel,
	                                Function<AppConfig, EncryptionMethod> algoFactory)
	{
		this.config = config;
		this.algoLabel = algoLabel;
		this.algoFactory = algoFactory;
	}

	/**
	 * Construit l'écran de sélection de fichier vidéo.
	 *
	 * @return la scène avec le FileChooser et la liste locale
	 */
	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.TOP_CENTER);
		root.setPadding(new Insets(30, 60, 30, 60));

		Label title = new Label("Chiffrement : " + algoLabel);
		title.setFont(Font.font("System", FontWeight.BOLD, 16));

		// --- Sélection via explorateur de fichiers ---
		Label browseTitle = new Label("Fichier sélectionné :");
		browseTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

		File initialFile = config.inputFile();
		Label selectedFileLabel = new Label(
				initialFile != null ? initialFile.getName() : "Aucun fichier sélectionné");

		Button browseButton = new Button("Parcourir…");

		HBox browseBox = new HBox(12, browseButton, selectedFileLabel);
		browseBox.setAlignment(Pos.CENTER_LEFT);

		// Fichier retenu : on passe par un tableau pour pouvoir muter dans les lambdas
		File[] selectedFile = { initialFile };

		// --- Bouton de lancement ---
		Button launchButton = new Button("Chiffrer →");
		launchButton.setFont(Font.font("System", FontWeight.BOLD, 14));
		launchButton.setPadding(new Insets(10, 40, 10, 40));
		launchButton.setDisable(initialFile == null);

		// Parcourir : ouvre un FileChooser
		browseButton.setOnAction(e ->
		{
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Sélectionner une vidéo");
			fileChooser.getExtensionFilters().add(
					new FileChooser.ExtensionFilter("Fichiers vidéo", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.m4v")
			);
			Stage stage = (Stage) browseButton.getScene().getWindow();
			File file = fileChooser.showOpenDialog(stage);
			if (file != null) {
				selectedFile[0] = file;
				selectedFileLabel.setText(file.getName());
				launchButton.setDisable(false);
			}
		});

		// Lancement du chiffrement avec le fichier retenu
		launchButton.setOnAction(e ->
		{
			AppConfig newConfig = new AppConfig(
					'C',
					selectedFile[0],
					config.outputDir(),
					config.offset(),
					config.step()
			);
			System.out.println("[VideoScramble] Lancement chiffrement : algo=" + algoLabel
					+ ", fichier=" + selectedFile[0].getAbsolutePath()
					+ " (exists=" + selectedFile[0].exists() + ", size=" + selectedFile[0].length() + ")");
			EncryptionMethod algo = algoFactory.apply(newConfig);
			SceneManager sm = SceneManager.getInstance();
			sm.register("scene:encryption", new EncryptionSceneController(newConfig, algo));
			sm.switchTo("scene:encryption");
		});

		root.getChildren().addAll(title, browseTitle, browseBox, launchButton, new GoHomeButton());

		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}
}
