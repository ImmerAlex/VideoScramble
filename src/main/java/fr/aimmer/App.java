package fr.aimmer;

import fr.aimmer.controller.EuclideSceneController;
import fr.aimmer.controller.HomeController;
import fr.aimmer.controller.FirstSceneController;
import fr.aimmer.controller.PearsonSceneController;
import fr.aimmer.listener.StageGlobalListener;
import fr.aimmer.ui.scene.SceneManager;
import javafx.application.Application;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class App extends Application
{
	public static void application(String[] args)
	{
		launch();
	}

	@Override
	public void start(Stage stage)
	{
		stage.addEventFilter(KeyEvent.KEY_PRESSED, StageGlobalListener::keyTyped);

		SceneManager sm = SceneManager.getInstance();
		sm.setStage(stage);

		sm.register("home", new HomeController());
		sm.register("scene:1", new FirstSceneController());
		sm.register("scene:euclide", new EuclideSceneController());
		sm.register("scene:pearson", new PearsonSceneController());

		sm.switchTo("home", true);
		stage.show();
	}
}
