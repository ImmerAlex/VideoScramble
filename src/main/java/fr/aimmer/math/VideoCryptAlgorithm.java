package fr.aimmer.math;

import org.opencv.core.Mat;

import java.util.Random;

/**
 * Chiffrement inspiré du système VideoCrypt (BSkyB, 1989) — "cut and rotate".
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
 * Algorithme involutif : la même opération avec la même graine chiffre et
 * déchiffre — le paramètre {@code inverse} de {@link #transformFrame} est
 * ignoré.
 */
public class VideoCryptAlgorithm extends AbstractFramePermutation
{
    // Nombre de positions de coupe possibles par ligne.
    // 256 correspond au choix historique de VideoCrypt.
    // TODO : exposer ce paramètre dans AppConfig si on veut le faire varier en démo.
    private static final int CUT_POSITIONS = 256;

    private final int seed;
    private int[] cuts;

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
        cuts = computeRowCutPoints(height, width, seed);
    }

    @Override
    protected void transformFrame(Mat source, Mat dest, boolean inverse)
    {
        // L'opération est sa propre inverse : aucun traitement spécial en mode inverse.
        applyCutAndRotate(source, dest, cuts);
    }

    /**
     * Pour chaque ligne, calcule le point de coupe (en pixels) à utiliser.
     * <p>
     * Le point est tiré dans [0, width) mais quantifié sur min(CUT_POSITIONS, width)
     * valeurs possibles : on garantit que tous les cuts générés sont < width
     * et > 0 (un cut à 0 ou width ne ferait rien).
     *
     * TODO :
     *   - Décider si la séquence de cuts est globale ou variable par frame.
     *     Globale = plus simple à attaquer/démontrer ; variable = plus sécurisé
     *     mais ne change pas grand-chose pédagogiquement.
     */
    // package-private pour les tests unitaires
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
     */
    private static void applyCutAndRotate(Mat source, Mat dest, int[] cuts)
    {
        // copyTo : alloue dest si nécessaire et couvre le cas des rares lignes
        // pour lesquelles cuts[i] vaudrait 0 ou width (rien à faire).
        source.copyTo(dest);

        int width = source.cols();
        for (int i = 0; i < cuts.length; i++) {
            int c = cuts[i];
            if (c <= 0 || c >= width) continue;

            // dest[0, w-c)  ← source[c, w)
            source.row(i).colRange(c, width).copyTo(dest.row(i).colRange(0, width - c));
            // dest[w-c, w)  ← source[0, c)
            source.row(i).colRange(0, c).copyTo(dest.row(i).colRange(width - c, width));
        }
    }
}
