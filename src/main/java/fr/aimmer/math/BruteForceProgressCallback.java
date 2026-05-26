/**
 * VideoScramble — Callback de progression pour les attaques par force brute.
 * <p>
 * Notifiée périodiquement pendant l'exploration de l'espace de clés.
 * Permet d'afficher en temps réel la meilleure clé trouvée et son score.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

@FunctionalInterface
public interface BruteForceProgressCallback
{
    /**
     * Notifie la progression de la recherche.
     *
     * @param done       nombre de clés testées
     * @param totalKeys  nombre total de clés
     * @param bestOffset meilleur offset trouvé jusqu'ici
     * @param bestStep   meilleur step trouvé jusqu'ici
     * @param bestScore  score de la meilleure clé (somme sur les frames échantillonnées)
     */
    void update(int done, int totalKeys, int bestOffset, int bestStep, double bestScore);
}
