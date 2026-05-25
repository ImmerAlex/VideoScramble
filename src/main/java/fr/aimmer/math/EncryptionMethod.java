/**
 * VideoScramble — Contrat commun à tous les algorithmes de chiffrement vidéo.
 * <p>
 * La clé spécifique à chaque algorithme (offset/step pour Nagravision, graine
 * pour Discret 11 ou VideoCrypt, etc.) est portée par l'état de l'instance
 * (passée au constructeur), pas par les méthodes — cela évite des signatures
 * divergentes entre implémentations.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import java.io.File;

public interface EncryptionMethod
{
    /**
     * Nom court de l'algorithme, utilisé pour l'UI et les logs.
     *
     * @return le nom d'affichage (ex: "Nagravision", "Discret 11")
     */
    String displayName();

    /**
     * Chiffre la vidéo d'entrée et écrit le résultat dans {@code outputDir}.
     * Le sous-dossier {@code generated/crypted} et le préfixe de fichier sont
     * gérés par l'implémentation.
     *
     * @param input     la vidéo source en clair
     * @param outputDir le dossier de sortie
     * @return le fichier chiffré produit
     */
    File encrypt(File input, File outputDir);

    /**
     * Déchiffre la vidéo d'entrée avec la clé portée par l'instance.
     * Le sous-dossier {@code generated/decrypted} et le préfixe de fichier sont
     * gérés par l'implémentation.
     *
     * @param input     la vidéo source chiffrée
     * @param outputDir le dossier de sortie
     * @return le fichier déchiffré produit
     */
    File decrypt(File input, File outputDir);
}
