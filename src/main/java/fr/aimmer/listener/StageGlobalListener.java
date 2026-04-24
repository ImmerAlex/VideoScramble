package fr.aimmer.listener;

import fr.aimmer.ui.scene.SceneManager;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class StageGlobalListener
{
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
