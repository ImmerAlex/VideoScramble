/**
 * VideoScramble — Écran de sélection du fichier chiffré à attaquer.
 * <p>
 * Similaire à {@link VideoSelectionController} mais pour le déchiffrement.
 * Propose un explorateur de fichiers et une liste des vidéos déjà chiffrées
 * disponibles dans le dossier {@code generated/crypted}. Le fichier retenu
 * est passé au controller de brute force.
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        // --- Liste des vidéos chiffrées disponibles ---
        Label listTitle = new Label("Vidéos chiffrées disponibles :");
        listTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        ListView<File> videoList = new ListView<>();
        videoList.setPrefHeight(260);
        List<File> cryptedFiles = findCryptedVideos();
        videoList.getItems().addAll(cryptedFiles);
        videoList.setCellFactory(e -> new ListCell<>()
        {
            @Override
            protected void updateItem(File item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Pré-sélection du premier fichier si dispo
        File[] selectedFile = { cryptedFiles.isEmpty() ? null : cryptedFiles.get(0) };
        if (selectedFile[0] != null)
            selectedFileLabel.setText(selectedFile[0].getName());

        // --- Bouton de lancement ---
        Button launchButton = new Button("Déchiffrer →");
        launchButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        launchButton.setPadding(new Insets(10, 40, 10, 40));
        // Désactivé tant qu'aucun fichier n'est sélectionné
        launchButton.setDisable(selectedFile[0] == null);

        // Parcourir
        browseButton.setOnAction(e ->
        {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une vidéo chiffrée");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Fichiers vidéo", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv")
            );
            // Le répertoire initial est le dossier des vidéos chiffrées s'il existe
            File cryptedDir = new File(config.outputDir(), "generated/crypted");
            if (cryptedDir.isDirectory()) fileChooser.setInitialDirectory(cryptedDir);
            Stage stage = (Stage) browseButton.getScene().getWindow();
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                selectedFile[0] = file;
                selectedFileLabel.setText(file.getName());
                videoList.getSelectionModel().clearSelection();
                launchButton.setDisable(false);
            }
        });

        // Clic dans la liste
        videoList.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) ->
        {
            if (newVal != null) {
                selectedFile[0] = newVal;
                selectedFileLabel.setText(newVal.getName());
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

        root.getChildren().addAll(browseTitle, browseBox, listTitle, videoList, launchButton, new GoHomeButton());

        return new Scene(root, Main.WIDTH, Main.HEIGHT);
    }

    /**
     * Cherche les vidéos déjà chiffrées dans le dossier {@code generated/crypted}.
     *
     * @return la liste des fichiers vidéo trouvés
     */
    private List<File> findCryptedVideos()
    {
        List<File> videos = new ArrayList<>();
        String[] videoExts = { ".mp4", ".avi", ".mkv", ".mov", ".wmv" };
        File cryptedDir = new File(config.outputDir(), "generated/crypted");

        if (!cryptedDir.isDirectory()) return videos;

        File[] found = cryptedDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            for (String ext : videoExts) {
                if (lower.endsWith(ext)) return true;
            }
            return false;
        });

        if (found != null) {
            Arrays.sort(found);
            videos.addAll(Arrays.asList(found));
        }

        return videos;
    }
}
