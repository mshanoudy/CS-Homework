import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.stream.IntStream;

public class LividFarm
{
    private static final int CURE_PLANT_SUM         = 233552;   // Expected summation of cure plant info box
    private static final int FERTILISE_SUM          = 209166;   // Expected summation of fertilise plant info box
    private static final int TAKE_PRODUCE_SUM       = 212262;   // Expected summation of take produce info box
    private static final int INFO_BOX_PIXEL_COUNT   = 14996;    // Pixel count of info box
    private static final int X_OFFSET               = 34;       // X offset from cursor to info box
    private static final int Y_OFFSET               = 36;       // Y offset from cursor to info box
    private static final int ASTRAL_MIN             = 6;        // Minimum amount of astral runes needed
    private static final int NATURE_MIN             = 4;        // Minimum amount of nature runes needed
    private static final int ANIMATION_DELAY        = 2000;     // ms for animations to finish
    private static final int MENU_DELAY             = 800;      // ms for menu to appear
    private static final int INFO_BOX_DELAY         = 700;      // ms for info box to appear

    /**
     * Dimensions for info box
     */
    private static final Dimension INFO_BOX = new Dimension(163, 23);

    /**
     * Array containing the type of strain corresponding to the appropriate livid patch for menu selection
     * @see <a href="http://runescape.wikia.com/wiki/Livid_Farm#Identifying_the_diseased_livid">Identifying the Diseased Livid</a>
     */
    private static final int [] LIVID_STRAINS   = new int[]   { 1, 1, 1, 2, 1, 4, 3, 2, 2, 2, 3, 4, 4, 4, 3 };

    /**
     * Array containing the coordinates of each livid patch starting from top left
     * @see <a href="http://runescape.wikia.com/wiki/Livid_Farm#Diseased_livid_positions">Diseased Livid Positions</a>
     */
    private static final Point [] LIVID_POINTS  = new Point[] { new Point(435, 380),
                                                                new Point(545, 386),
                                                                new Point(646, 388),
                                                                new Point(751, 383),
                                                                new Point(858, 388),
                                                                new Point(407, 539),
                                                                new Point(520, 540),
                                                                new Point(627, 540),
                                                                new Point(740, 539),
                                                                new Point(853, 543),
                                                                new Point(382, 703),
                                                                new Point(493, 707),
                                                                new Point(614, 706),
                                                                new Point(724, 710),
                                                                new Point(845, 706),
                                                                new Point(1302, 293) }; // Produce stall

    /**
     * Array containing the coordinates of each menu choices corresponding to the appropriate livid strain
     */
    private static final Point [] MENU_POINTS   = new Point[] { new Point(54, 501),
                                                                new Point(147, 497),
                                                                new Point(52, 614),
                                                                new Point(142, 616) };

    private Bot bot;
    private int astralRune = 0;
    private int natureRune = 0;

    /**
     * Constructor
     * @param astralRune amount of astral runes in inventory
     * @param natureRune amount of nature runes in inventory
     * @throws Exception
     */
    public LividFarm(int astralRune, int natureRune) throws Exception
    {
        this.bot = new Bot();
        this.astralRune = astralRune;
        this.natureRune = natureRune;
    }

    /**
     * Runs the Livid Farm routine
     */
    public void start()
    {
        int x, y, sum;
        Point point;
        BufferedImage image;

        while (hasRunes())
            for (int i = 0; i < LIVID_POINTS.length; i++)
            {
                if (!hasRunes()) break;

                point = LIVID_POINTS[i];

                bot.cursorMove(point);
                x = (int)point.getX() + X_OFFSET;
                y = (int)point.getY() + Y_OFFSET;
                image = bot.screenShot(new Point(x, y), INFO_BOX);
                sum   = sumImage(image);

                switch (sum)
                {
                    case CURE_PLANT_SUM:
                        curePlant(i, point);
                        break;
                    case FERTILISE_SUM:
                        fertilisePatch(point);
                        break;
                    case TAKE_PRODUCE_SUM:
                        takeProduce(point);
                        break;
                    default:
                        break;
                }

                bot.wait(INFO_BOX_DELAY);
            }
    }

    /**
     * Decrements rune count based on spell cast
     * @param spell the spell cast
     */
    private void castSpell(String spell)
    {
        switch (spell)
        {
            case "Cure Plant":
                astralRune--;
                break;
            case "String Jewellery":
                astralRune -= 2;
                break;
            case "Fertile Soil":
                astralRune -= 3;
                natureRune -= 2;
                break;
            default:
                break;
        }
    }

    /**
     * Are there still enough runes left in inventory?
     * @return if min runes
     */
    private boolean hasRunes()
    {
        return (astralRune > ASTRAL_MIN) && (natureRune > NATURE_MIN);
    }

    /**
     * Have character take produce and bunch it
     * @param point coordinate of produce stall
     */
    private void takeProduce(Point point)
    {
        bot.cursorPress(point);
        bot.wait(3000);
        animationPress(KeyEvent.VK_M);
        castSpell("String Jewellery");
        animationPress(KeyEvent.VK_M);
        castSpell("String Jewellery");
        bot.cursorPress(1177, 942);
        bot.wait(4500);
        bot.cursorPress(459, 476);
        bot.wait(4100);
    }

    /**
     * Have character cure a livid plant
     * @param i index of livid plant
     * @param point coordinate of livid plant
     */
    private void curePlant(int i, Point point)
    {
        bot.cursorPress(point);
        bot.wait(MENU_DELAY);
        castSpell("Cure Plant");
        animationPress(getMenuPoint(i));
    }

    /**
     * Have character fertilise an empty patch
     * @param point coordinate of empty patch
     */
    private void fertilisePatch(Point point)
    {
        castSpell("Fertile Soil");
        animationPress(point);
    }

    /**
     * Gets coordinate of menu option for livid strain
     * @param i index of livid plant strain
     * @return coordinate of menu option for livid strain
     */
    private Point getMenuPoint(int i)
    {
        return MENU_POINTS[LIVID_STRAINS[i] - 1];
    }

    /**
     * Have character press key and wait for animation to finish
     * @param key the key to press
     */
    private void animationPress(int key)
    {
        bot.keyPress(key);
        bot.wait(ANIMATION_DELAY);
    }

    /**
     * Have character press cursor and wait for animation to finish
     * @param point coordinate for cursor
     */
    private void animationPress(Point point)
    {
        bot.cursorPress(point);
        bot.wait(ANIMATION_DELAY);
    }

    /**
     * Sums the pixel information of image (red, blue, green, alpha)
     * @param image image to be summed
     * @return summation of image information
     */
    private int sumImage(BufferedImage image)
    {
        int[] pixels = new int[INFO_BOX_PIXEL_COUNT];
        image.getData().getPixels(0, 0, INFO_BOX.width, INFO_BOX.height, pixels);
        return IntStream.of(pixels).sum();
    }

    /**
     * Driver
     * TODO: make a proper Main class and/or command line args
     * @param args command line args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        LividFarm lividFarm = new LividFarm(4681, 3831);
        lividFarm.start();
    }
}
