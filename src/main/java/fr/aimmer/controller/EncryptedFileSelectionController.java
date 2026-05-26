/**
 * VideoScramble — Écran de sélection du fichier chiffré à attaquer.
 * <p>
 * Propose un explorateur de fichiers (FileChooser) pour choisir la vidéo chiffrée.
 * Le fichier retenu est passé au controller de brute force.
 * <p>
 * Cette scène est enregistrée dynamiquement par {@link DecryptionSelectionController}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
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

public class EncryptedFileSelectionController implements Controller
{
    private final AppConfig config;
    private final String targetSceneId;
    private final Function<AppConfig, Controller> controllerFactory;

    /**
     * @param config              la config de session
     * @param targetSceneId       l'ID de scène où naviguer après sélection
     * @param controllerFactory   fabrique du controller cible à partir de la config
     */
    public EncryptedFileSelectionController(AppConfig config, String targetSceneId,
                                            Function<AppConfig, Controller> controllerFactory)
    {
        this.config = config;
        this.targetSceneId = targetSceneId;
        this.controllerFactory = controllerFactory;
    }

    /**
     * Construit l'écran de sélection du fichier chiffré.
     *
     * @return la scène de sélection
     */
    @Override
    public Scene get()
    {
        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30, 60, 30, 60));

        // --- Sélection via explorateur de fichiers ---
        Label browseTitle = new Label("Fichier sélectionné :");
        browseTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label selectedFileLabel = new Label("Aucun fichier sélectionné");

        Button browseButton = new Button("Parcourir…");

        HBox browseBox = new HBox(12, browseButton, selectedFileLabel);
        browseBox.setAlignment(Pos.CENTER_LEFT);

        File[] selectedFile = { null };

        // --- Bouton de lancement ---
        Button launchButton = new Button("Déchiffrer →");
        launchButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        launchButton.setPadding(new Insets(10, 40, 10, 40));
        launchButton.setDisable(true);

        // Parcourir
        browseButton.setOnAction(e ->
        {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une vidéo chiffrée");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers vidéo", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv", "*.m4v")
            );
            File cryptedDir = new File(config.outputDir(), "generated/crypted");
            if (cryptedDir.isDirectory()) fileChooser.setInitialDirectory(cryptedDir);
            Stage stage = (Stage) browseButton.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                selectedFile[0] = file;
                selectedFileLabel.setText(file.getName());
                launchButton.setDisable(false);
            }
        });

        // Lancement de l'attaque
        launchButton.setOnAction(e ->
        {
            AppConfig newConfig = new AppConfig(
                    'D',
                    selectedFile[0],
                    config.outputDir(),
                    config.offset(),
                    config.step()
            );
            SceneManager sm = SceneManager.getInstance();
            sm.register(targetSceneId, controllerFactory.apply(newConfig));
            sm.switchTo(targetSceneId);
        });

        root.getChildren().addAll(browseTitle, browseBox, launchButton, new GoHomeButton());

        return new Scene(root, Main.WIDTH, Main.HEIGHT);
    }
}
