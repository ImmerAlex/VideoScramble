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

/**
 * Scène générique de résultat pour une attaque par force brute.
 * <p>
 * Paramétrée par le {@link DecryptionMethod} à appliquer — ajouter une nouvelle
 * métrique ne nécessite pas de nouveau controller, juste une nouvelle entrée
 * dans {@link DecryptionSelectionController}.
 */
public class BruteForceSceneController implements Controller
{
    private final AppConfig config;
    private final DecryptionMethod attack;

    public BruteForceSceneController(AppConfig config, DecryptionMethod attack)
    {
        this.config = config;
        this.attack = attack;
    }

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

        Task<List<MediaView>> task = new Task<>()
        {
            @Override
            protected List<MediaView> call() throws Exception
            {
                BruteForceResult result = attack.attack(
                        fileToDecrypt,
                        config.outputDir(),
                        done -> updateProgress(done, attack.totalKeys())
                );

                MediaView encryptedView = MediaViewFactory.getMediaView(fileToDecrypt);
                MediaView decryptedView = MediaViewFactory.getMediaView(result.outputFile());
                // Transmet la clé trouvée via le message du Task pour l'afficher ensuite
                updateMessage(result.offset() + ":" + result.step());
                return List.of(encryptedView, decryptedView);
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(event -> {
            List<MediaView> videos = task.getValue();
            root.getChildren().clear();

            Label methodLabel = new Label("Méthode : " + attack.displayName());
            methodLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

            String[] keyParts = task.getMessage().split(":");
            Label offsetLabel = new Label("OFFSET: " + keyParts[0]);
            offsetLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            Label stepLabel = new Label("STEP: " + keyParts[1]);
            stepLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

            HBox topLabels = new HBox(20, offsetLabel, stepLabel);
            topLabels.setAlignment(Pos.CENTER);
            root.getChildren().addAll(methodLabel, topLabels);

            HBox videoBox = new HBox(20, videos.get(0), videos.get(1));
            videoBox.setAlignment(Pos.CENTER);
            root.getChildren().add(videoBox);

            videos.get(0).fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
            videos.get(1).fitWidthProperty().bind(videoBox.widthProperty().subtract(videoBox.getSpacing()).divide(2));
            videos.get(0).setPreserveRatio(true);
            videos.get(1).setPreserveRatio(true);

            GoHomeButton goHomeButton = new GoHomeButton();
            root.getChildren().add(goHomeButton);
            VBox.setMargin(goHomeButton, new Insets(20, 0, 0, 0));
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
