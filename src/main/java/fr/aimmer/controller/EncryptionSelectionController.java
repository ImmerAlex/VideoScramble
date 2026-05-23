package fr.aimmer.controller;

import fr.aimmer.AppConfig;
import fr.aimmer.Main;
import fr.aimmer.math.Discret11Algorithm;
import fr.aimmer.math.EncryptionMethod;
import fr.aimmer.math.NagravisionAlgorithm;
import fr.aimmer.math.VideoCryptAlgorithm;
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
import java.util.function.Function;

public class EncryptionSelectionController implements Controller
{
	// La graine pour les algorithmes basés sur PRNG (Discret 11, VideoCrypt) est
	// dérivée de (offset, step) → 256 × 128 = 32 768 graines distinctes, ce qui
	// aligne leur espace de clés sur celui de Nagravision pour la démo.
	private static final List<EncryptionType> TYPES = List.of(
			new EncryptionType("Nagravision",
					"Permutation des lignes par blocs de puissance de 2",
					"""
					Permutation des lignes de chaque frame par blocs de puissance de 2.
					
					La permutation dépend d'un couple (offset, step). L'offset varie
					à chaque frame (offset + numéro de frame modulo 256), produisant
					un brouillage dynamique différent d'une image à l'autre.
					
					Clé : offset ∈ [0, 255] × step ∈ [0, 127] = 32 768 combinaisons.
					Symétrique : la même opération avec les mêmes paramètres chiffre
					et déchiffre (l'algorithme est son propre inverse).
					""",
					c -> new NagravisionAlgorithm(c.offset(), c.step())),
			new EncryptionType("Discret 11",
					"Décalage horizontal pseudo-aléatoire des lignes (3 niveaux, wrap-around)",
					"""
					Décale chaque ligne horizontalement d'un nombre pseudo-aléatoire
					de pixels, parmi 3 niveaux possibles (0, +4 ou +8 pixels).
					Wrap-around : les pixels qui sortent à droite réapparaissent à gauche.
					
					La séquence de décalages varie à chaque frame (graine + numéro
					de frame), inspiré de l'alternance pair/impair du Discret 11 original.
					
					Clé : graine entière dérivée de offset × 128 + step
					(32 768 graines distinctes).
					Symétrique : le déchiffrement applique les décalages inverses.
					""",
					c -> new Discret11Algorithm(c.offset() * 128 + c.step())),
			new EncryptionType("VideoCrypt",
					"Cut-and-rotate : échange des deux moitiés à un point de coupe pseudo-aléatoire",
					"""
					Coupe chaque ligne à une position pseudo-aléatoire, puis échange
					les deux moitiés (cut-and-rotate). 256 positions de coupe quantifiées
					sur la largeur de l'image.
					
					La séquence de coupes varie à chaque frame (graine + numéro
					de frame). L'image chiffrée est visuellement méconnaissable.
					
					Clé : graine entière dérivée de offset × 128 + step.
					Involutif : appliquer l'algorithme deux fois avec la même graine
					restaure l'image originale (pas d'opération inverse distincte).
					""",
					c -> new VideoCryptAlgorithm(c.offset() * 128 + c.step()))
	);

	private final AppConfig config;

	public EncryptionSelectionController(AppConfig config)
	{
		this.config = config;
	}

	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(40));

		Label title = new Label("Méthode de chiffrement");
		title.setFont(Font.font("System", FontWeight.BOLD, 18));
		root.getChildren().add(title);

		for (EncryptionType type : TYPES) {
			root.getChildren().add(buildCard(type));
		}

		root.getChildren().add(new GoHomeButton());

		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}

	private VBox buildCard(EncryptionType type)
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
			sm.register("scene:encryption:video-selection",
					new VideoSelectionController(config, type.label(), type.algoFactory()));
			sm.switchTo("scene:encryption:video-selection");
		});

		VBox card = new VBox(6, header, descLabel, launchButton);
		card.setAlignment(Pos.CENTER_LEFT);
		card.setPadding(new Insets(12, 20, 12, 20));
		card.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 6; -fx-background-radius: 6;");
		card.setMaxWidth(600);

		return card;
	}

	// Ajouter ici les futurs algorithmes de chiffrement (1 ligne dans TYPES suffit)
	private record EncryptionType(
			String label,
			String description,
			String infoText,
			Function<AppConfig, EncryptionMethod> algoFactory
	)
	{
	}
}
