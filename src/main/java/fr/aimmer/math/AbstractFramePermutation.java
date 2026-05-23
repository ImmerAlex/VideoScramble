package fr.aimmer.math;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;

/**
 * Base commune des algorithmes de chiffrement qui opèrent frame par frame
 * sur une vidéo (permutation de lignes, décalages, cut-and-rotate, etc.).
 * <p>
 * Factorise l'ouverture/fermeture d'OpenCV ({@link VideoCapture}/{@link VideoWriter})
 * et la boucle frame par frame. Les sous-classes n'ont qu'à implémenter :
 * <ul>
 *   <li>{@link #prepareForResolution(int, int)} — appelé une fois avant la boucle
 *       pour pré-calculer ce qui dépend de la résolution (mapping de lignes,
 *       séquence de décalages, points de coupe…).</li>
 *   <li>{@link #transformFrame(Mat, Mat, boolean)} — appelé pour chaque frame ;
 *       {@code inverse=true} en mode déchiffrement.</li>
 *   <li>{@link #filePrefix(boolean)} — préfixe du fichier de sortie
 *       (ex. {@code "encrypted_nagra_"} / {@code "decrypted_nagra_"}).</li>
 * </ul>
 */
public abstract class AbstractFramePermutation implements EncryptionMethod
{
    @Override
    public final File encrypt(File input, File outputDir)
    {
        File parent = input.getParentFile();
        if (parent == null) parent = new File(".");
        return process(input, parent, filePrefix(false), false);
    }

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
     */
    protected abstract String filePrefix(boolean inverse);

    /**
     * Pré-calcule l'état dépendant de la résolution (mapping de lignes, séquence
     * de décalages, points de coupe…). Appelé une fois par {@link #process}
     * juste avant la boucle frame par frame.
     */
    protected abstract void prepareForResolution(int width, int height);

    /**
     * Transforme une frame source en une frame destination.
     *
     * @param inverse {@code false} en chiffrement, {@code true} en déchiffrement.
     *                Les implémentations involutives (ex. VideoCrypt) peuvent
     *                ignorer ce paramètre.
     */
    protected abstract void transformFrame(Mat source, Mat dest, boolean inverse);

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

        int useFps = fps > 0 ? fps : 30;

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
