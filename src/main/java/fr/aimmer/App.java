package fr.aimmer;

import fr.aimmer.controller.EncryptionSceneController;
import fr.aimmer.controller.EuclideSceneController;
import fr.aimmer.controller.HomeController;
import fr.aimmer.controller.PearsonSceneController;
import fr.aimmer.listener.StageGlobalListener;
import fr.aimmer.ui.scene.SceneManager;
import javafx.application.Application;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class App extends Application
{
	private static AppConfig config;

	public static void application(AppConfig appConfig, String[] args)
	{
		config = appConfig;
		launch(App.class, args);
	}

	public static AppConfig getConfig()
	{
		return config;
	}

	@Override
	public void start(Stage stage)
	{
		stage.addEventFilter(KeyEvent.KEY_PRESSED, StageGlobalListener::keyTyped);

		SceneManager sm = SceneManager.getInstance();
		sm.setStage(stage);

		sm.register("home", new HomeController());
		sm.register("scene:1", new EncryptionSceneController(config));
		sm.register("scene:euclide", new EuclideSceneController(config));
		sm.register("scene:pearson", new PearsonSceneController(config));

		sm.switchTo("home", true);
		stage.show();
	}
}
