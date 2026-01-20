package fr.aimmer;

import fr.aimmer.controller.HomeController;
import fr.aimmer.controller.SettingsController;
import fr.aimmer.ui.scene.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Arrays;

public class Main extends Application
{
	public static final int WIDTH  = 800;
	public static final int HEIGHT = 600;

	public static void main(String[] args)
	{
		System.out.println("Debut du code");
		System.out.println(Arrays.toString(args));
		for (String arg : args) {
			System.out.println(arg);
		}

		launch();
	}

	@Override
	public void start(Stage stage) throws Exception
	{
		SceneManager sm = SceneManager.getInstance();
		sm.setStage(stage);

		sm.register("home", new HomeController());
		sm.register("settings", new SettingsController());

		sm.switchTo("home", true);
		stage.show();
	}
}
