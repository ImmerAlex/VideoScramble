/**
 * VideoScramble — Singleton de gestion des scènes JavaFX.
 * <p>
 * Implémentation thread-safe (double-checked locking) avec cache optionnel
 * des scènes. Les contrôleurs sont enregistrés via une factory {@link java.util.function.Supplier}
 * et instanciés à la demande.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.ui.scene;

import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class SceneManager
{
	/** Instance unique, volatile pour la visibilité inter-threads */
	private static volatile SceneManager                 instance;
	private final           Map<String, Supplier<Scene>> factories = new ConcurrentHashMap<>();
	private final           Map<String, Scene>           cache     = new ConcurrentHashMap<>();
	private                 Stage                        stage;

	private SceneManager()
	{
		// Singleton : constructeur privé
	}

	/**
	 * Retourne l'instance unique du SceneManager (double-checked locking).
	 *
	 * @return l'instance
	 */
	public static SceneManager getInstance()
	{
		if (instance == null) {
			synchronized (SceneManager.class) {
				if (instance == null) instance = new SceneManager();
			}
		}
		return instance;
	}

	/**
	 * @return le stage principal, ou {@code null} s'il n'a pas encore été initialisé
	 */
	public Stage getStage()
	{
		return stage;
	}

	/**
	 * Définit le stage principal. À appeler une fois au lancement.
	 *
	 * @param stage le stage primaire JavaFX
	 */
	public void setStage(Stage stage)
	{
		this.stage = stage;
	}

	/**
	 * Enregistre une factory de scène pour un ID donné.
	 *
	 * @param id      l'identifiant de la scène (ex: "home")
	 * @param factory la factory qui produit la scène (souvent un {@link fr.aimmer.controller.Controller})
	 */
	public void register(String id, Supplier<Scene> factory)
	{
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(factory, "factory must not be null");
		factories.put(id, factory);
	}

	/**
	 * Pré-charge une scène dans le cache (appelle la factory une fois).
	 *
	 * @param id l'identifiant de la scène
	 * @return la scène créée
	 */
	public Scene preload(String id)
	{
		return cache.computeIfAbsent(id, this::createSceneUnchecked);
	}

	/**
	 * Supprime une scène du cache et des factories.
	 *
	 * @param id l'identifiant de la scène
	 */
	public void remove(String id)
	{
		cache.remove(id);
		factories.remove(id);
	}

	/**
	 * Récupère une scène (depuis le cache si disponible, sinon la crée).
	 *
	 * @param id l'identifiant de la scène
	 * @return la scène
	 */
	public Scene getScene(String id)
	{
		Scene s = cache.get(id);
		return ( s != null ) ? s : createSceneUnchecked(id);
	}

	/**
	 * Change la scène affichée sur un stage donné.
	 *
	 * @param stage      le stage cible
	 * @param id         l'identifiant de la scène
	 * @param cacheScene si {@code true}, la scène est gardée en cache
	 */
	public void switchTo(Stage stage, String id, boolean cacheScene)
	{
		Objects.requireNonNull(stage, "stage must not be null");
		Scene scene = cacheScene ? preload(id) : getScene(id);
		if (scene == null) throw new IllegalArgumentException("No scene registered for id: " + id);
		stage.setScene(scene);
	}

	/**
	 * Change la scène sur le stage principal (sans cache).
	 *
	 * @param id l'identifiant de la scène
	 */
	public void switchTo(String id)
	{
		if (stage == null) throw new IllegalStateException("SceneManager not initialized with primary Stage");
		switchTo(stage, id, false);
	}

	/**
	 * Change la scène sur le stage principal.
	 *
	 * @param id         l'identifiant de la scène
	 * @param cacheScene si {@code true}, la scène est gardée en cache
	 */
	public void switchTo(String id, boolean cacheScene)
	{
		if (stage == null) throw new IllegalStateException("SceneManager not initialized with primary Stage");
		switchTo(stage, id, cacheScene);
	}

	/**
	 * Crée une scène à partir de sa factory, sans vérifier le cache.
	 *
	 * @param id l'identifiant de la scène
	 * @return la scène créée
	 * @throws IllegalArgumentException si aucune factory n'est enregistrée
	 * @throws IllegalStateException    si la factory retourne null
	 */
	private Scene createSceneUnchecked(String id)
	{
		Supplier<Scene> factory = factories.get(id);
		if (factory == null) throw new IllegalArgumentException("No factory registered for id: " + id);
		Scene scene = factory.get();
		if (scene == null) throw new IllegalStateException("Factory returned null for id: " + id);
		return scene;
	}
}
