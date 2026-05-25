/**
 * VideoScramble — Chiffrement inspiré du système VideoCrypt (BSkyB, 1989) — "cut and rotate".
 * <p>
 * Principe : pour chaque ligne, on choisit un point de coupe pseudo-aléatoire,
 * puis on échange les deux moitiés. Visuellement spectaculaire : l'image
 * chiffrée est totalement méconnaissable alors qu'aucune information n'est
 * perdue.
 * <p>
 * Pour borner l'espace de clés (utile pour la démo et pour permettre une
 * éventuelle attaque brute force), on quantifie le point de coupe sur N
 * positions discrètes au lieu de la largeur complète.
 * <p>
 * La graine effective varie à chaque frame : {@code seed + frameIndex}.
 * La séquence de coupes n'est donc jamais la même d'une frame à l'autre.
 * <p>
 * Algorithme involutif : la même opération avec la même graine chiffre et
 * déchiffre — le paramètre {@code inverse} de {@link #transformFrame} est
 * ignoré.
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import org.opencv.core.Mat;

import java.util.Random;

public class VideoCryptAlgorithm extends AbstractFramePermutation
{
    /** Nombre de positions de coupe possibles par ligne. 256 = choix historique de VideoCrypt. */
    private static final int CUT_POSITIONS = 256;

    private final int seed;
    private int frameIndex;

    /**
     * @param seed la graine du PRNG (dérivée de offset*128+step dans l'UI)
     */
    public VideoCryptAlgorithm(int seed)
    {
        this.seed = seed;
    }

    @Override
    public String displayName()
    {
        return "VideoCrypt";
    }

    @Override
    protected String filePrefix(boolean inverse)
    {
        return inverse ? "decrypted_vc_" : "encrypted_vc_";
    }

    @Override
    protected void prepareForResolution(int width, int height)
    {
        this.frameIndex = 0;
    }

    @Override
    protected void transformFrame(Mat source, Mat dest, boolean inverse)
    {
        // La graine varie avec la frame
        int[] cuts = computeRowCutPoints(source.rows(), source.cols(), seed + frameIndex);
        // L'opération est sa propre inverse : on ignore le paramètre 'inverse'
        applyCutAndRotate(source, dest, cuts);
        frameIndex++;
    }

    /**
     * Pour chaque ligne, calcule le point de coupe (en pixels) à utiliser.
     * <p>
     * Le point est tiré dans [0, width) mais quantifié sur min(CUT_POSITIONS, width)
     * valeurs possibles. On garantit que tous les cuts sont strictement > 0 et < width
     * (un cut à 0 ou width ne ferait rien).
     *
     * @param height nombre de lignes
     * @param width  largeur de l'image en pixels
     * @param seed   la graine effective (seed + frameIndex)
     * @return tableau des points de coupe par ligne
     */
    // package-private pour les tests
    static int[] computeRowCutPoints(int height, int width, int seed)
    {
        int[] cuts = new int[height];
        Random rng = new Random(seed);

        // Si width < CUT_POSITIONS, on a moins de positions distinctes que la valeur
        // historique de VideoCrypt — c'est acceptable pour la démo.
        int positions = Math.min(CUT_POSITIONS, Math.max(2, width));
        int binWidth = Math.max(1, width / positions);
        for (int i = 0; i < height; i++) {
            // bin dans [1, positions-1] pour éviter cut=0 (qui ne ferait rien)
            int bin = 1 + rng.nextInt(positions - 1);
            cuts[i] = Math.min(bin * binWidth, width - 1);
        }
        return cuts;
    }

    /**
     * Applique le cut-and-rotate sur chaque ligne :
     * <pre>
     *   ligne_originale : [A | B]   (coupée à la position cut)
     *   ligne_chiffrée  : [B | A]
     * </pre>
     * L'opération est sa propre inverse (involutive) : appliquer une seconde
     * fois avec le même point de coupe restaure la ligne.
     *
     * @param source frame d'entrée
     * @param dest   frame de sortie
     * @param cuts   points de coupe par ligne
     */
    private static void applyCutAndRotate(Mat source, Mat dest, int[] cuts)
    {
        // copyTo : alloue dest si nécessaire et couvre les rares lignes où cut=0
        source.copyTo(dest);

        int width = source.cols();
        for (int i = 0; i < cuts.length; i++) {
            int c = cuts[i];
            if (c <= 0 || c >= width) continue; // rien à faire

            // dest[0, width-c)  ← source[c, width)
            source.row(i).colRange(c, width).copyTo(dest.row(i).colRange(0, width - c));
            // dest[width-c, width) ← source[0, c)
            source.row(i).colRange(0, c).copyTo(dest.row(i).colRange(width - c, width));
        }
    }
}
