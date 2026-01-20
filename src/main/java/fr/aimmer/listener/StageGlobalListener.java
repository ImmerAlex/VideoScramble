package fr.aimmer.listener;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class StageGlobalListener
{
	public static void keyTyped(KeyEvent event) {
		if (event.getCode() == KeyCode.ESCAPE) {
			System.exit(0);
		}

		SceneManager sm = SceneManager.getInstance();

		if (event.getCode() == KeyCode.BACK_SPACE) {
			sm.switchTo("home", true);
		}
	}
}
