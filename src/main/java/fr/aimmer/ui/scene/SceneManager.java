package fr.aimmer.ui.scene;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class SceneManager
{
	// volatile pour une meilleur visibilité entre threads
	private static volatile SceneManager                 instance;
	private final           Map<String, Supplier<Scene>> factories = new ConcurrentHashMap<>();
	private final           Map<String, Scene>           cache     = new ConcurrentHashMap<>();
	private                 Stage                        stage;

	private SceneManager()
	{
	}

	public static SceneManager getInstance()
	{
		if (instance == null) {
			synchronized (SceneManager.class) {
				if (instance == null) instance = new SceneManager();
			}
		}
		return instance;
	}

	public Stage getStage()
	{
		return stage;
	}

	public void setStage(Stage stage)
	{
		this.stage = stage;
	}

	public void register(String id, Supplier<Scene> factory)
	{
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(factory, "factory must not be null");
		factories.put(id, factory);
	}

	public Scene preload(String id)
	{
		return cache.computeIfAbsent(id, this::createSceneUnchecked);
	}

	public void remove(String id)
	{
		cache.remove(id);
		factories.remove(id);
	}

	public Scene getScene(String id)
	{
		Scene s = cache.get(id);
		return ( s != null ) ? s : createSceneUnchecked(id);
	}

	public void switchTo(Stage stage, String id, boolean cacheScene)
	{
		Objects.requireNonNull(stage, "stage must not be null");
		Scene scene = cacheScene ? preload(id) : getScene(id);
		if (scene == null) throw new IllegalArgumentException("No scene registered for id: " + id);
		stage.setScene(scene);
	}

	public void switchTo(String id)
	{
		if (stage == null) throw new IllegalStateException("SceneManager not initialized with primary Stage");
		switchTo(stage, id, false);
	}

	public void switchTo(String id, boolean cacheScene)
	{
		if (stage == null) throw new IllegalStateException("SceneManager not initialized with primary Stage");
		switchTo(stage, id, cacheScene);
	}

	private Scene createSceneUnchecked(String id)
	{
		Supplier<Scene> factory = factories.get(id);
		if (factory == null) throw new IllegalArgumentException("No factory registered for id: " + id);
		Scene scene = factory.get();
		if (scene == null) throw new IllegalStateException("Factory returned null for id: " + id);
		return scene;
	}
}