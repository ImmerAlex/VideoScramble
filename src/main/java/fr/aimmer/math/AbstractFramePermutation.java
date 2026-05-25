/**
 * VideoScramble — Base commune des algorithmes de chiffrement qui opèrent frame par frame
 * sur une vidéo (permutation de lignes, décalages, cut-and-rotate, etc.).
 * <p>
 * Factorise l'ouverture/fermeture d'OpenCV ({@link org.opencv.videoio.VideoCapture}/{@link org.opencv.videoio.VideoWriter})
 * et la boucle frame par frame. Les sous-classes n'ont qu'à implémenter :
 * <ul>
 *   <li>{@link #prepareForResolution(int, int)} — appelé une fois avant la boucle
 *       pour pré-calculer ce qui dépend de la résolution (mapping de lignes,
 *       séquence de décalages, points de coupe…).</li>
 *   <li>{@link #transformFrame(org.opencv.core.Mat, org.opencv.core.Mat, boolean)} — appelé pour chaque frame ;
 *       {@code inverse=true} en mode déchiffrement.</li>
 *   <li>{@link #filePrefix(boolean)} — préfixe du fichier de sortie
 *       (ex. {@code "encrypted_nagra_"} / {@code "decrypted_nagra_"}).</li>
 * </ul>
 *
 * @author Alex IMMER & Olivier MARAVAL, Groupe Alt1
 */
package fr.aimmer.math;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;

public abstract class AbstractFramePermutation implements EncryptionMethod
{
    /**
     * Chiffre la vidéo : applique la permutation frame par frame et écrit le résultat.
     */
    @Override
    public final File encrypt(File input, File outputDir)
    {
        File parent = input.getParentFile();
        if (parent == null) parent = new File(".");
        return process(input, parent, filePrefix(false), false);
    }

    /**
     * Déchiffre la vidéo : applique la permutation inverse et écrit le résultat.
     */
    @Override
    public final File decrypt(File input, File outputDir)
    {
        File parent = input.getParentFile();
        if (parent == null) parent = new File(".");
        return process(input, parent, filePrefix(true), true);
    }

    /**
     * Préfixe du fichier de sortie. Appelé deux fois : une fois en chiffrement
     * ({@code inverse=false}) et une fois en déchiffrement ({@code inverse=true}).
     *
     * @param inverse {@code true} si mode déchiffrement
     * @return le préfixe (ex: {@code "encrypted_"} ou {@code "decrypted_"})
     */
    protected abstract String filePrefix(boolean inverse);

    /**
     * Pré-calcule l'état dépendant de la résolution (mapping de lignes, séquence
     * de décalages, points de coupe…). Appelé une fois par {@link #process}
     * juste avant la boucle frame par frame.
     *
     * @param width  largeur de la vidéo en pixels
     * @param height hauteur de la vidéo en pixels
     */
    protected abstract void prepareForResolution(int width, int height);

    /**
     * Transforme une frame source en une frame destination.
     *
     * @param source  la frame d'entrée (ne pas modifier)
     * @param dest    la frame de sortie (allouée par l'appelant)
     * @param inverse {@code false} en chiffrement, {@code true} en déchiffrement.
     *                Les implémentations involutives (ex. VideoCrypt) peuvent
     *                ignorer ce paramètre.
     */
    protected abstract void transformFrame(Mat source, Mat dest, boolean inverse);

    /**
     * Cœur du traitement : ouvre la vidéo, boucle sur les frames, écrit le résultat.
     *
     * @param inputFile le fichier vidéo source
     * @param outputDir le dossier de sortie
     * @param prefix    le préfixe pour le nom du fichier de sortie
     * @param inverse   {@code true} = mode déchiffrement
     * @return le fichier produit
     */
    private File process(File inputFile, File outputDir, String prefix, boolean inverse)
    {
        System.out.println("[VideoScramble] Traitement : input=" + inputFile.getAbsolutePath()
                + " (" + inputFile.length() + " octets)"
                + ", outputDir=" + outputDir.getAbsolutePath()
                + ", prefix=" + prefix
                + ", mode=" + (inverse ? "déchiffrement" : "chiffrement"));

        if (!inputFile.isFile())
            throw new RuntimeException("Fichier vidéo introuvable : " + inputFile.getAbsolutePath());

        outputDir.mkdirs();

        VideoCapture capture = new VideoCapture(inputFile.getAbsolutePath());

        if (!capture.isOpened())
            throw new RuntimeException(
                    "Impossible d'ouvrir la vidéo : " + inputFile.getAbsolutePath()
                    + "\nVérifiez que le fichier existe et que le codec est supporté par OpenCV."
            );

        File outputFile = new File(outputDir, prefix + inputFile.getName());
        int width  = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        int fps    = (int) capture.get(Videoio.CAP_PROP_FPS);
        int total  = (int) capture.get(Videoio.CAP_PROP_FRAME_COUNT);

        System.out.println("[VideoScramble] Vidéo ouverte : " + width + "x" + height
                + ", " + fps + " fps, " + total + " frames");

        if (width <= 0 || height <= 0)
            throw new RuntimeException(
                    "Résolution invalide (" + width + "x" + height + ") pour : " + inputFile.getName()
            );

        // Si le FPS n'est pas détecté, on prend 30 par défaut
        int useFps = fps > 0 ? fps : 30;

        // On essaie d'abord avc1 (H.264), fallback sur mp4v si indisponible
        VideoWriter writer = new VideoWriter(
                outputFile.getAbsolutePath(),
                VideoWriter.fourcc('a', 'v', 'c', '1'),
                useFps,
                new Size(width, height)
        );

        if (!writer.isOpened())
        {
            System.out.println("[VideoScramble] Codec avc1 indisponible, fallback sur mp4v.");
            writer = new VideoWriter(
                    outputFile.getAbsolutePath(),
                    VideoWriter.fourcc('m', 'p', '4', 'v'),
                    useFps,
                    new Size(width, height)
            );
        }

        if (!writer.isOpened())
            throw new RuntimeException(
                    "Impossible de créer la vidéo de sortie : " + outputFile.getAbsolutePath()
                    + "\nVérifiez les permissions d'écriture et la disponibilité des codecs avc1/mp4v."
            );

        System.out.println("[VideoScramble] Writer ouvert : " + outputFile.getAbsolutePath());

        // Initialisation dépendante de la résolution (une seule fois)
        prepareForResolution(width, height);

        Mat frame  = new Mat();
        Mat output = new Mat();

        int frameCount = 0;
        while (capture.read(frame))
        {
            transformFrame(frame, output, inverse);
            writer.write(output);
            frameCount++;
        }

        capture.release();
        writer.release();

        System.out.println("[VideoScramble] " + frameCount + " frames traitées, sortie : "
                + outputFile.getAbsolutePath() + " (" + outputFile.length() + " octets)");

        if (frameCount == 0)
            throw new RuntimeException(
                    "Aucune frame n'a pu être décodée depuis : " + inputFile.getName()
                    + "\nLe codec vidéo n'est peut-être pas supporté par cette version d'OpenCV."
            );

        return outputFile;
    }
}
