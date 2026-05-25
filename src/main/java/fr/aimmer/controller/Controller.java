/**
 * VideoScramble — Interface fonctionnelle pour les contrôleurs d'écran.
 * <p>
 * Chaque écran de l'application implémente cette interface : sa méthode {@link #get()}
 * construit l'UI programmatiquement (pas de FXML) et retourne une {@link javafx.scene.Scene}
 * prête à être affichée. Étend {@link java.util.function.Supplier} pour pouvoir être
 * enregistrée directement dans le {@link fr.aimmer.ui.scene.SceneManager}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.controller;

import javafx.scene.Scene;

import java.util.function.Supplier;


@FunctionalInterface
public interface Controller extends Supplier<Scene>
{
	/**
	 * Construit la scène et retourne-la. Appelée à chaque navigation (sauf si
	 * la scène est mise en cache).
	 *
	 * @return la scène JavaFX prête à être affichée
	 */
	@Override
	Scene get();
}
