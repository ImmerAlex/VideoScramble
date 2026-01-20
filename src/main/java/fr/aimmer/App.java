package fr.aimmer;

import fr.aimmer.controller.HomeController;
import fr.aimmer.controller.SettingsController;
import fr.aimmer.ui.scene.SceneManager;
import javafx.application.Application;
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
		SceneManager sm = SceneManager.getInstance();
		sm.setStage(stage);

		sm.register("home", new HomeController());
		sm.register("settings", new SettingsController());

		sm.switchTo("home", true);
		stage.show();
	}
}
