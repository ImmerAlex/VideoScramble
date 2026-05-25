/**
 * VideoScramble — Bouton réutilisable de retour à l'accueil.
 * <p>
 * Affiche "Home" et redirige vers la scène d'accueil via le {@link fr.aimmer.ui.scene.SceneManager}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.view;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.control.Button;

public class GoHomeButton extends Button
{
	/**
	 * Construit le bouton "Home" avec son action de navigation.
	 */
	public GoHomeButton()
	{
		super("Home");
		this.setOnAction(e -> SceneManager.getInstance().switchTo("home", true));
	}
}
