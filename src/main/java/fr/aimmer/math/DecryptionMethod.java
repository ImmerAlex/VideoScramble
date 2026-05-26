/**
 * VideoScramble — Contrat commun aux attaques de déchiffrement.
 * <p>
 * Une implémentation = une stratégie d'attaque complète (espace de clés
 * exploré + métrique de scoring). Pour Nagravision, voir {@link NagravisionBruteForce},
 * qui se paramétrise par une {@link RowScoringFunction}.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import java.io.File;

public interface DecryptionMethod
{
    /**
     * Nom court de l'attaque, utilisé pour l'UI et les logs.
     *
     * @return le nom d'affichage (ex: "Euclide", "Pearson")
     */
    String displayName();

    /**
     * Attaque la vidéo chiffrée et produit la vidéo déchiffrée dans {@code outputDir}.
     *
     * @param encryptedFile    la vidéo chiffrée à attaquer
     * @param outputDir        le dossier où écrire le résultat
     * @param progressCallback notifié à chaque clé testée avec le meilleur score courant, peut être {@code null}
     * @return le résultat contenant le fichier déchiffré et la clé trouvée
     */
    BruteForceResult attack(File encryptedFile, File outputDir, BruteForceProgressCallback progressCallback);

    /**
     * Total de clés que l'attaque parcourt, utilisé pour calibrer une ProgressBar côté UI.
     *
     * @return le nombre total de combinaisons explorées
     */
    int totalKeys();
}
