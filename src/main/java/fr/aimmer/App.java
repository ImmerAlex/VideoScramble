/**
 * VideoScramble — Application JavaFX principale.
 * <p>
 * Enregistre les scènes racines dans le {@link fr.aimmer.ui.scene.SceneManager}
 * et initialise le {@link javafx.stage.Stage} primaire. Reçoit sa configuration
 * via le champ statique {@code config} positionné par {@link Main} avant le
 * {@code launch()}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer;

import fr.aimmer.controller.*;
import fr.aimmer.listener.StageGlobalListener;
import fr.aimmer.ui.scene.SceneManager;
import javafx.application.Application;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class App extends Application
{
    /** Configuration de session, positionnée avant le launch() JavaFX */
    private static AppConfig config;

    /**
     * Point d'entrée appelé par {@link Main#main} : stocke la config et lance JavaFX.
     *
     * @param appConfig la configuration de session
     * @param args      les arguments passés à {@link #launch}
     */
    public static void application(AppConfig appConfig, String[] args)
    {
        config = appConfig;
        launch(App.class, args);
    }

    /**
     * Retourne la configuration de session. Disponible partout après le lancement.
     *
     * @return la {@link AppConfig} courante
     */
    public static AppConfig getConfig()
    {
        return config;
    }

    /**
     * Initialise la fenêtre principale : écouteurs globaux, enregistrement des
     * scènes, navigation vers l'écran d'accueil.
     *
     * @param stage le stage primaire fourni par JavaFX
     */
    @Override
    public void start(Stage stage)
    {
        // Échap = quitter, Backspace = retour accueil
        stage.addEventFilter(KeyEvent.KEY_PRESSED, StageGlobalListener::keyTyped);

        SceneManager sm = SceneManager.getInstance();
        sm.setStage(stage);

        // Scènes toujours présentes (pas de paramétrage dynamique)
        sm.register("home", new HomeController());

        sm.register("scene:encryption:selection", new EncryptionSelectionController(config));

        sm.register("scene:decryption:selection", new DecryptionSelectionController(config));

        // On commence par l'accueil
        sm.switchTo("home", true);
        stage.show();
    }
}
