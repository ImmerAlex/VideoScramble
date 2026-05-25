/**
 * VideoScramble — Écouteur global des touches clavier.
 * <p>
 * ESC = quitter l'application, BACKSPACE = retour à l'accueil.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.listener;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class StageGlobalListener
{
	/**
	 * Callback appelé à chaque pression de touche sur le stage principal.
	 *
	 * @param event l'événement clavier
	 */
	public static void keyTyped(KeyEvent event)
	{
		if (event.getCode() == KeyCode.ESCAPE) {
			System.exit(0);
		}

		if (event.getCode() == KeyCode.BACK_SPACE) {
			SceneManager.getInstance().switchTo("home", true);
		}
	}
}
