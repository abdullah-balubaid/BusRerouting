import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * <p><b>Project Name:</b> Bus Rerouting</p>
 * <p><b>Class Name:</b> Icon</p>
 * <p><b>Purpose:</b> The picture of one thing on the map (a bus, or a stop). An {@code Icon} knows its
 * file, finds it, loads it only once however many icons use that file, optionally tints it with a
 * colour, and hands out a copy shrunk to the size the map is drawn at. If the file cannot be found
 * it gives {@code null}, so the owner draws its plain shapes instead.</p>
 *
 * <p>The picture files are looked for in an {@code assets} folder inside the project folder, and
 * failing that anywhere below the project folder (up to four levels deep).</p>
 *
 * @author Ahmad Balfakeeh
 * @author Abdullah Balubaid
 * @author Salam Kanno
 * @author Omar AbuSabaah
 * @version v1.0 11/26/26
 */
public class Icon {
    private static final String ASSETS_FOLDER = "assets";   // Folder inside the project that holds the pictures
    private static final int SEARCH_DEPTH = 4;               // How many folder levels below the project are searched

    // Every picture file is loaded once and shared. A file that could not be found is stored as null,
    // so the disk is not searched again every frame.
    private static final java.util.Map<String, Image> loadedPictures = new HashMap<>();

    private final String fileName;     // Name of the picture file, e.g. "StandardBusIcon.png"
    private final double scale;        // How much bigger (or smaller) than the standard icon size this icon is drawn
    private final Color tint;          // Colour to tint the picture with, or null to use it as it is
    private Image readyPicture;        // The picture at full size with the tint applied (made on first use)
    private boolean prepared = false;  // True once readyPicture has been made (or found missing)
    private Image shownPicture;        // The picture shrunk to the size currently drawn
    private int shownSize = -1;        // The standard size shownPicture was made for

    /**
     * Creates an icon that is drawn as the picture is, without tinting.
     *
     * @param fileName name of the picture file
     * @param scale    size multiplier relative to the standard icon size (1.0 = standard)
     */
    public Icon(String fileName, double scale) {
        this(fileName, scale, null);
    }

    /**
     * Creates an icon whose picture is tinted: the body takes {@code tint} and the windows turn black.
     * The tint is applied once, to a copy of the full-size picture, the first time the icon is drawn.
     *
     * @param fileName name of the picture file
     * @param scale    size multiplier relative to the standard icon size (1.0 = standard)
     * @param tint     colour for the body, or {@code null} for no tint
     */
    public Icon(String fileName, double scale, Color tint) {
        this.fileName = fileName;
        this.scale = scale;
        this.tint = tint;
    }

    /**
     * Returns the picture ready to draw at a given size. The picture is shrunk smoothly from its full
     * size, and the result is kept until a different size is asked for.
     *
     * @param standardSize the standard icon size in pixels (this icon's own scale is applied on top)
     * @return the picture, or {@code null} if the file could not be found or read
     */
    public Image getImage(double standardSize) {
        if (!prepared) {
            Image original = load(fileName);
            readyPicture = (original == null || tint == null) ? original : applyTint(original, tint);
            prepared = true;
        }

        if (readyPicture == null) {
            return null;
        }

        int size = (int) standardSize;
        if (size != shownSize) {
            shownPicture = shrink(readyPicture, size * scale);
            shownSize = size;
        }
        return shownPicture;
    }

    /**
     * Returns how much bigger or smaller than the standard icon size this icon is drawn.
     *
     * @return the size multiplier (1.0 = standard)
     */
    public double getScale() {
        return scale;
    }

    /**
     * Loads a picture file, or reuses it if it was loaded before.
     *
     * @param fileName name of the picture file
     * @return the picture at full size, or {@code null} if it could not be found or read
     */
    private static Image load(String fileName) {
        if (loadedPictures.containsKey(fileName)) {
            return loadedPictures.get(fileName);
        }

        Image picture = null;
        File file = findFile(fileName);
        if (file == null) {
            System.out.println("Icon not found: " + fileName);
        } else {
            try (InputStream stream = new FileInputStream(file)) {
                Image read = new Image(stream);
                picture = read.isError() ? null : read;
            } catch (IOException e) {
                picture = null;
            }
        }

        loadedPictures.put(fileName, picture);
        return picture;
    }

    /**
     * Looks for a picture file: first in the {@code assets} folder of the project, then anywhere below
     * the project folder.
     *
     * @param fileName name of the picture file
     * @return the file, or {@code null} if it was not found
     */
    private static File findFile(String fileName) {
        File projectFolder = new File(System.getProperty("user.dir"));

        File inAssets = new File(new File(projectFolder, ASSETS_FOLDER), fileName);
        if (inAssets.isFile()) {
            return inAssets;
        }

        return searchFolder(projectFolder, fileName, SEARCH_DEPTH);
    }

    /**
     * Searches a folder and its sub-folders (ignoring hidden ones) for a file, ignoring upper/lower case.
     *
     * @param folder    folder to search
     * @param fileName  name of the file wanted
     * @param depthLeft how many more levels of sub-folders may be searched
     * @return the file, or {@code null} if it was not found
     */
    private static File searchFolder(File folder, String fileName, int depthLeft) {
        File[] items = (folder == null) ? null : folder.listFiles();

        if (items == null || depthLeft < 0) {
            return null;
        }

        for (File item : items) {
            if (item.isFile() && item.getName().equalsIgnoreCase(fileName)) {
                return item;
            }
        }

        for (File item : items) {
            if (item.isDirectory() && !item.getName().startsWith(".")) {
                File found = searchFolder(item, fileName, depthLeft - 1);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Shrinks an image so its larger side equals {@code box} pixels, keeping the aspect ratio. Each new
     * pixel is the average of the source pixels it covers (alpha-weighted), which looks much smoother
     * than letting the canvas squash a large picture.
     *
     * @param source the image to shrink
     * @param box    wanted size of the larger side, in pixels
     * @return the shrunken image, or {@code source} itself if it is already that small
     */
    private Image shrink(Image source, double box) {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        double factor = box / Math.max(width, height);
        if (factor >= 1) {
            return source;
        }

        int newWidth = Math.max(1, (int) Math.round(width * factor));
        int newHeight = Math.max(1, (int) Math.round(height * factor));
        int[] pixels = new int[width * height];
        source.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        int[] small = new int[newWidth * newHeight];

        for (int ny = 0; ny < newHeight; ny++) {
            int y0 = ny * height / newHeight;
            int y1 = Math.max(y0 + 1, (ny + 1) * height / newHeight);
            for (int nx = 0; nx < newWidth; nx++) {
                int x0 = nx * width / newWidth;
                int x1 = Math.max(x0 + 1, (nx + 1) * width / newWidth);

                long a = 0, r = 0, g = 0, b = 0;
                int count = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int p = pixels[y * width + x];
                        int alpha = alphaOf(p);
                        a += alpha;
                        r += (long) ((p >> 16) & 255) * alpha;
                        g += (long) ((p >> 8) & 255) * alpha;
                        b += (long) (p & 255) * alpha;
                        count++;
                    }
                }
                int alpha = (int) (a / count);
                int red = a == 0 ? 0 : (int) (r / a);
                int green = a == 0 ? 0 : (int) (g / a);
                int blue = a == 0 ? 0 : (int) (b / a);
                small[ny * newWidth + nx] = (alpha << 24) | (red << 16) | (green << 8) | blue;
            }
        }

        WritableImage result = new WritableImage(newWidth, newHeight);
        result.getPixelWriter().setPixels(0, 0, newWidth, newHeight, PixelFormat.getIntArgbInstance(), small, 0, newWidth);
        return result;
    }

    /**
     * Flood-fills transparent exterior pixels from the image border so interior/body pixels (and the
     * windows, which turn black) can be recolored with {@code tintColor} while preserving anti-aliased edge shading and transparency.
     *
     * @param picture  source icon {@link Image}
     * @param tintColor the colour to tint the body with
     * @return newly created tinted {@link WritableImage}
     */
    private Image applyTint(Image picture, Color tintColor) {
        int width = (int) picture.getWidth();
        int height = (int) picture.getHeight();
        int totalPixels = width * height;
        int[] pixels = new int[totalPixels];

        picture.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        boolean[] outside = new boolean[totalPixels];
        int[] toVisit = new int[totalPixels];
        int toVisitCount = 0;

        // Seed flood fill from semi-/fully transparent border pixels
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;

            boolean onBorder = (x == 0 || y == 0 || x == width - 1 || y == height - 1);

            if (onBorder && alphaOf(pixels[i]) < 128) {
                outside[i] = true;
                toVisit[toVisitCount++] = i;
            }
        }

        // Propagate exterior mask across connected low-alpha pixels in 4-neighborhood
        while (toVisitCount > 0) {
            int i = toVisit[--toVisitCount];
            int x = i % width;
            int y = i / width;
            int next;

            next = i - 1;
            if (x > 0 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }

            next = i + 1;
            if (x < width - 1 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }

            next = i - width;
            if (y > 0 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }

            next = i + width;
            if (y < height - 1 && !outside[next] && alphaOf(pixels[next]) < 128) {
                outside[next] = true;
                toVisit[toVisitCount++] = next;
            }
        }

        int[] painted = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            int x = i % width;
            int y = i / width;
            int alpha = alphaOf(pixels[i]);

            boolean touchesOutside = (x > 0 && outside[i - 1] || (x < width - 1 && outside[i + 1])
                    || (y > 0 && outside[i - width]) || (y < height - 1 && outside[i + width]));

            int newAlpha;
            double shade;
            if (outside[i] || touchesOutside) {
                // Keep exterior and outer edge anti-aliasing transparent
                newAlpha = alpha;
                shade = 1;
            } else {
                // Solidify interior pixels and modulate brightness by original alpha
                newAlpha = 255;
                shade = alpha / 255.0;
            }

            int red = (int) (tintColor.getRed() * shade * 255);
            int green = (int) (tintColor.getGreen() * shade * 255);
            int blue = (int) (tintColor.getBlue() * shade * 255);
            painted[i] = (newAlpha << 24) | (red << 16) | (green << 8) | blue;
        }

        WritableImage result = new WritableImage(width, height);
        result.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), painted, 0, width);
        return result;
    }

    /**
     * Extracts the 8-bit alpha channel ({@code 0..255}) from an ARGB packed integer pixel.
     *
     * @param pixel packed ARGB pixel value
     * @return alpha component in {@code [0, 255]}
     */
    private int alphaOf(int pixel) {
        return (pixel >>> 24) & 255;
    }
}
