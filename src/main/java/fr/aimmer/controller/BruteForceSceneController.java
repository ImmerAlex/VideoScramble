/**
 * VideoScramble — Scène de résultat d'attaque par force brute.
 * <p>
 * Affiche côte à côte la vidéo chiffrée et la vidéo déchiffrée, avec la clé
 * retrouvée (offset, step). L'attaque est lancée dans un {@link javafx.concurrent.Task}
 * pour ne pas bloquer l'UI, avec une barre de progression.
 * <p>
 * Controller générique paramétré par le {@link fr.aimmer.math.DecryptionMethod}
 * à appliquer. Ajouter une nouvelle métrique ne nécessite pas de nouveau
 * controller, juste une entrée dans {@link DecryptionSelectionController}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.BruteForceResult;
import fr.aimmer.math.DecryptionMethod;
import fr.aimmer.utils.MediaViewFactory;
import fr.aimmer.view.GoHomeButton;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.util.List;

public class BruteForceSceneController implements Controller
{
    private final AppConfig config;
    private final DecryptionMethod attack;

    /**
     * @param config la configuration de session
     * @param attack la méthode d'attaque à utiliser
     */
    public BruteForceSceneController(AppConfig config, DecryptionMethod attack)
    {
        this.config = config;
        this.attack = attack;
    }

    /**
     * Construit la scène de brute force.
     * <p>
     * Affiche une barre de progression pendant l'attaque, puis les deux vidéos
     * (chiffrée / déchiffrée) avec la clé retrouvée.
     *
     * @return la scène de résultat d'attaque
     */
    @Override
    public Scene get()
    {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        Label statusLabel = new Label(
                "Méthode : " + attack.displayName()
                + " — recherche de la clé sur " + attack.totalKeys() + " combinaisons…");
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);

        root.getChildren().addAll(statusLabel, progressBar);

        Scene scene = new Scene(root, Main.WIDTH, Main.HEIGHT);

        File fileToDecrypt = config.inputFile();

        // Tâche de fond : brute force
        Task<List<File>> task = new Task<>()
        {
            @Override
            protected List<File> call() throws Exception
            {
                System.out.println("[VideoScramble] Début attaque : méthode=" + attack.displayName()
                        + ", input=" + fileToDecrypt.getAbsolutePath()
                        + ", totalKeys=" + attack.totalKeys());

                BruteForceResult result = attack.attack(
                        fileToDecrypt,
                        config.outputDir(),
                        // callback de progression : met à jour la barre
                        done -> updateProgress(done, attack.totalKeys())
                );

                System.out.println("[VideoScramble] Attaque terminée : clé=(" + result.offset()
                        + "," + result.step() + "), fichier=" + result.outputFile().getAbsolutePath()
                        + " (" + result.outputFile().length() + " octets)");

                // On triche un peu : on passe la clé via le message de la Task
                updateMessage(result.offset() + ":" + result.step());
                return List.of(fileToDecrypt, result.outputFile());
            }
        };

        // Binding barre de progression ↔ progression de la tâche
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(event -> {
            List<File> files = task.getValue();
            root.getChildren().clear();

            // Vidéo chiffrée (gauche) et déchiffrée (droite)
            MediaView encryptedView = MediaViewFactory.getMediaView(files.get(0));
            MediaView decryptedView = MediaViewFactory.getMediaView(files.get(1));

            Label methodLabel = new Label("Méthode : " + attack.displayName());
            methodLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

            // Récupération de la clé depuis le message de la Task
            String[] keyParts = task.getMessage().split(":");
            Label offsetLabel = new Label("OFFSET: " + keyParts[0]);
            offsetLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            Label stepLabel = new Label("STEP: " + keyParts[1]);
            stepLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

            HBox topLabels = new HBox(20, offsetLabel, stepLabel);
            topLabels.setAlignment(Pos.CENTER);
            root.getChildren().addAll(methodLabel, topLabels);

            HBox videoBox = new HBox(20, encryptedView, decryptedView);
            videoBox.setAlignment(Pos.CENTER);
            root.getChildren().add(videoBox);

            encryptedView.fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
            decryptedView.fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
            encryptedView.setPreserveRatio(true);
            decryptedView.setPreserveRatio(true);

            GoHomeButton goHomeButton = new GoHomeButton();
            root.getChildren().add(goHomeButton);
            VBox.setMargin(goHomeButton, new Insets(20, 0, 0, 0));
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("[VideoScramble] ERREUR attaque : " + ex.getMessage());
            ex.printStackTrace(System.err);
            root.getChildren().clear();
            root.getChildren().addAll(
                    new Label("Erreur : " + ex.getMessage()),
                    new GoHomeButton()
            );
        });

        new Thread(task).start();

        return scene;
    }
}
