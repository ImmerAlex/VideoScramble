package fr.aimmer;

import fr.aimmer.controller.*;
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

        sm.register("scene:encryption:selection", new EncryptionSelectionController(config));

        sm.register("scene:decryption:selection", new DecryptionSelectionController(config));

        sm.switchTo("home", true);
        stage.show();
    }
}
