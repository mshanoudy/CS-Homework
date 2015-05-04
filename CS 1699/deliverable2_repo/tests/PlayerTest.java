import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

/**
 * Tests for {@link Player}.
 *
 * @author Mark Shanoudy
 */
public class PlayerTest
{
    private final ByteArrayOutputStream consoleOutput = new ByteArrayOutputStream();

    @Before
    public void setUpStream()
    {
        System.setOut(new PrintStream(consoleOutput));
    }

    @After
    public void cleanUpStream()
    {
        System.setOut(null);
    }

    /**
     * Test for hasAllItems().
     * Should return true if all items are present. False is any items are missing.
     * All possible combinations are tested.
     */
    @Test
    public void testHasAllItems()
    {
        Player testPlayer;

        testPlayer = new Player(false, false, false);   // No items
        assertFalse(testPlayer.hasAllItems());
        testPlayer = new Player(false, true, false);    // Cream
        assertFalse(testPlayer.hasAllItems());
        testPlayer = new Player(false, false, true);    // Coffee
        assertFalse(testPlayer.hasAllItems());
        testPlayer = new Player(false, true, true);     // Cream and coffee
        assertFalse(testPlayer.hasAllItems());
        testPlayer = new Player(true, false, false);    // Sugar
        assertFalse(testPlayer.hasAllItems());
        testPlayer = new Player(true, true, false);     // Sugar and cream
        assertFalse(testPlayer.hasAllItems());
        testPlayer = new Player(true, false, true);     // Sugar and coffee
        assertFalse(testPlayer.hasAllItems());

        testPlayer = new Player(true, true, true);      // All items
        assertTrue(testPlayer.hasAllItems());
    }

    @Test
    public void testGetSugar()
    {
        Player testPlayer = new Player(false, true, true);

        String successString = "You found some sweet sugar!\r\n";

        assertFalse(testPlayer.hasAllItems());
        testPlayer.getSugar();
        assertEquals(consoleOutput.toString(), successString);
        assertTrue(testPlayer.hasAllItems());
    }

    @Test
    public void testGetCream()
    {
        Player testPlayer = new Player(true, false, true);

        String successString = "You found some creamy cream!\r\n";

        assertFalse(testPlayer.hasAllItems());
        testPlayer.getCream();
        assertEquals(consoleOutput.toString(), successString);
        assertTrue(testPlayer.hasAllItems());
    }

    @Test
    public void testGetCoffee()
    {
        Player testPlayer = new Player(true, true, false);

        String successString = "You found some caffeinated coffee!\r\n";

        assertFalse(testPlayer.hasAllItems());
        testPlayer.getCoffee();
        assertEquals(consoleOutput.toString(), successString);
        assertTrue(testPlayer.hasAllItems());
    }



    @Test
    public void testShowInventory()
    {
        Player testPlayer;

        String successString =  "You have a cup of delicious coffee.\r\n" +
                                "You have some fresh cream.\r\n" +
                                "You have some tasty sugar.\r\n";
        String errorString   =  "YOU HAVE NO COFFEE!\r\n" +
                                "YOU HAVE NO CREAM!\r\n" +
                                "YOU HAVE NO SUGAR!\r\n";

        testPlayer = new Player(true, true, true);
        testPlayer.showInventory();
        assertEquals(consoleOutput.toString(), successString);

        consoleOutput.reset();

        testPlayer = new Player(false, false, false);
        testPlayer.showInventory();
        assertEquals(consoleOutput.toString(), errorString);
    }

    @Test
    public void testDrink()
    {
        Player testPlayer;

        testPlayer = new Player(true, true, true);
        assertTrue(testPlayer.drink());
        testPlayer = new Player(false, false, false);
        assertFalse(testPlayer.drink());
    }
}