package fr.aimmer.math;

import java.io.File;
import java.util.function.IntConsumer;

/**
 * Contrat commun aux attaques de déchiffrement.
 * <p>
 * Une implémentation = une stratégie d'attaque complète (espace de clés
 * exploré + métrique de scoring). Pour Nagravision, voir {@link NagravisionBruteForce},
 * qui se paramétrise par une {@link RowScoringFunction}.
 */
public interface DecryptionMethod
{
    /**
     * Nom court de l'attaque, utilisé pour l'UI et les logs.
     */
    String displayName();

    /**
     * Attaque la vidéo chiffrée et produit la vidéo déchiffrée dans {@code outputDir}.
     *
     * @param progressCallback notifié avec le nombre de clés testées, peut être {@code null}.
     *                         Le total dépend de l'implémentation (ex. 256×128 = 32 768 pour Nagravision).
     */
    BruteForceResult attack(File encryptedFile, File outputDir, IntConsumer progressCallback);

    /**
     * Total de clés que l'attaque parcourt, utilisé pour calibrer une ProgressBar côté UI.
     */
    int totalKeys();
}
