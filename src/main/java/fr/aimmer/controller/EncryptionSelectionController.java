/**
 * VideoScramble — Écran de sélection de la méthode de chiffrement.
 * <p>
 * Affiche une carte par algorithme disponible (Nagravision, Discret 11, VideoCrypt).
 * Chaque carte présente un titre, une description courte, un bouton d'info détaillée
 * et un bouton pour lancer le processus.
 * <p>
 * C'est ici qu'on choisit quel algo sera utilisé pour le chiffrement. La clé
 * (offset, step) vient de l'{@link fr.aimmer.AppConfig} et est partagée entre
 * tous les algos — pour Discret 11 et VideoCrypt, on dérive une graine à partir
 * de {@code offset * 128 + step}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
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
	// La graine pour les PRNG (Discret 11, VideoCrypt) est dérivée de (offset, step)
	// → offset * 128 + step = 32 768 graines distinctes, alignées sur l'espace de
	// clés de Nagravision. Pratique pour la démo vu que l'utilisateur règle toujours
	// offset/step quel que soit l'algo choisi.
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

	/**
	 * @param config la configuration de session (porte offset/step pour tous les algos)
	 */
	public EncryptionSelectionController(AppConfig config)
	{
		this.config = config;
	}

	/**
	 * Construit l'écran de sélection de chiffrement.
	 *
	 * @return la scène avec les cartes des 3 algorithmes
	 */
	@Override
	public Scene get()
	{
		VBox root = new VBox(20);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(40));

		Label title = new Label("Méthode de chiffrement");
		title.setFont(Font.font("System", FontWeight.BOLD, 18));
		root.getChildren().add(title);

		// Une carte par algo
		for (EncryptionType type : TYPES) {
			root.getChildren().add(buildCard(type));
		}

		root.getChildren().add(new GoHomeButton());

		return new Scene(root, Main.WIDTH, Main.HEIGHT);
	}

	/**
	 * Construit la carte UI pour un type de chiffrement donné.
	 *
	 * @param type les infos de l'algo (nom, description, factory...)
	 * @return une VBox contenant la carte complète
	 */
	private VBox buildCard(EncryptionType type)
	{
		Label nameLabel = new Label(type.label());
		nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

		// Petit bouton "?" pour les détails techniques
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

		// Le bouton principal : lance la sélection de fichier pour cet algo
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

	// Stocke les infos d'un algo pour l'affichage dans la carte.
	// Ajouter un nouvel algo ici = juste une ligne dans TYPES.
	private record EncryptionType(
			String label,
			String description,
			String infoText,
			Function<AppConfig, EncryptionMethod> algoFactory
	)
	{
	}
}
