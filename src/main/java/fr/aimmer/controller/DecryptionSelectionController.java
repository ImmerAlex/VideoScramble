package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.DecryptionMethod;
import fr.aimmer.math.NagravisionBruteForce;
import fr.aimmer.math.scoring.EuclideanScoring;
import fr.aimmer.math.scoring.L1Scoring;
import fr.aimmer.math.scoring.PearsonScoring;
import fr.aimmer.ui.scene.SceneManager;
import fr.aimmer.view.GoHomeButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class DecryptionSelectionController implements Controller
{
    // NagravisionBruteForce est sans état mutable : une instance partagée par
    // type de scoring est sûre, même si plusieurs scènes s'enchaînent.
    private static final List<DecryptionType> TYPES = List.of(
            new DecryptionType("Euclide",
                    "Force brute — distance euclidienne (L2) entre lignes adjacentes",
                    """
                    Attaque par force brute sur l'espace de clés Nagravision :
                    256 × 128 = 32 768 clés (offset, step) explorées.
                    
                    Pour chaque clé candidate, la permutation inverse est appliquée
                    virtuellement et la distance euclidienne √(Σ Δ²) entre lignes
                    adjacentes est calculée. La clé qui minimise cette distance
                    (image la plus « lisse ») est retenue.
                    
                    Le score est moyenné sur 5 frames pour éviter les faux positifs.
                    
                    Compatible UNIQUEMENT avec le chiffrement Nagravision.
                    Ne fonctionne PAS sur Discret 11 ni VideoCrypt.
                    """,
                    new NagravisionBruteForce("Euclide", new EuclideanScoring())),
            new DecryptionType("Pearson",
                    "Force brute — corrélation de Pearson (insensible aux décalages de luminosité)",
                    """
                    Attaque par force brute sur l'espace de clés Nagravision :
                    256 × 128 = 32 768 clés (offset, step) explorées.
                    
                    Pour chaque clé candidate, la permutation inverse est appliquée
                    virtuellement et la corrélation de Pearson entre lignes adjacentes
                    est mesurée. Score = 1 − r (plus r est proche de 1, plus le score
                    est bas). La clé qui minimise la somme des scores est retenue.
                    
                    Avantage vs Euclide : insensible aux décalages de luminosité
                    (dégradés verticaux, vignettage). Le score est moyenné sur
                    5 frames pour éviter les faux positifs.
                    
                    Compatible UNIQUEMENT avec le chiffrement Nagravision.
                    Ne fonctionne PAS sur Discret 11 ni VideoCrypt.
                    """,
                    new NagravisionBruteForce("Pearson", new PearsonScoring())),
            new DecryptionType("Variation totale",
                    "Force brute — somme des |Δ| (norme L1), favorise les images lisses",
                    """
                    Attaque par force brute sur l'espace de clés Nagravision :
                    256 × 128 = 32 768 clés (offset, step) explorées.
                    
                    Pour chaque clé candidate, la permutation inverse est appliquée
                    virtuellement et la somme des valeurs absolues des différences
                    Σ |Δ| (norme L1) entre lignes adjacentes est calculée.
                    La clé qui minimise cette variation totale est retenue.
                    
                    Plus robuste aux outliers qu'Euclide (pas de carré) et moins
                    coûteuse (pas de racine). Favorise les images « lisses par
                    morceaux ». Score moyenné sur 5 frames.
                    
                    Compatible UNIQUEMENT avec le chiffrement Nagravision.
                    Ne fonctionne PAS sur Discret 11 ni VideoCrypt.
                    """,
                    new NagravisionBruteForce("Variation totale", new L1Scoring()))
    );
    private final AppConfig config;

    public DecryptionSelectionController(AppConfig config)
    {
        this.config = config;
    }

    @Override
    public Scene get()
    {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        Label title = new Label("Méthode de déchiffrement");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label warning = new Label(
                "Force brute sur l'espace de clés de Nagravision (offset × step). "
                + "Une vidéo chiffrée par Discret 11 ou VideoCrypt ne pourra pas être restaurée ici : "
                + "chaque chiffrement nécessite son propre attaquant.");
        warning.setWrapText(true);
        warning.setMaxWidth(600);
        warning.setStyle("-fx-font-style: italic; -fx-text-fill: #555555;");

        root.getChildren().addAll(title, warning);

        for (DecryptionType type : TYPES) {
            root.getChildren().add(buildCard(type));
        }

        root.getChildren().add(new GoHomeButton());

        return new Scene(root, Main.WIDTH, Main.HEIGHT);
    }

    private VBox buildCard(DecryptionType type)
    {
        Label nameLabel = new Label(type.label());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button infoButton = new Button("?");
        infoButton.setStyle("-fx-font-size: 11px; -fx-padding: 1 6 1 6; -fx-min-width: 24px;");
        infoButton.setOnAction(e ->
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(type.label() + " — Fonctionnement");
            alert.setHeaderText(type.label());
            alert.setContentText(type.infoText());
            alert.showAndWait();
        });

        HBox header = new HBox(8, nameLabel, infoButton);
        header.setAlignment(Pos.CENTER_LEFT);

        Label descLabel = new Label(type.description());

        Button launchButton = new Button("Sélectionner →");
        launchButton.setOnAction(e ->
        {
            SceneManager sm = SceneManager.getInstance();
            sm.register("scene:decryption:file-selection",
                    new EncryptedFileSelectionController(config, "scene:decryption:result",
                            cfg -> new BruteForceSceneController(cfg, type.attack())));
            sm.switchTo("scene:decryption:file-selection");
        });

        VBox card = new VBox(6, header, descLabel, launchButton);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 20, 12, 20));
        card.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 6; -fx-background-radius: 6;");
        card.setMaxWidth(600);

        return card;
    }

    // Ajouter ici les futurs algorithmes de déchiffrement (1 ligne dans TYPES suffit)
    private record DecryptionType(
            String label,
            String description,
            String infoText,
            DecryptionMethod attack
    )
    {
    }
}
