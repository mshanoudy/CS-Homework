import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link Game}.
 *
 * @author Mark Shanoudy
 */
public class GameTest
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
     * Test for doSomething().
     * Should recognize six commands - "N" to go North, "S" to go South, "L" to Look for items, "I" for Inventory,
     * "H" for Help, or "D" to Drink. Commands should be case-insensitive. Unknown commands should print a message
     * to the console.
     */
    @Test
    public void testDoSomething()
    {
        Player mockPlayer = mock(Player.class);
        House  mockHouse  = mock(House.class);
        Game   testGame   = new Game(mockPlayer, mockHouse);

        doReturn(true).when(mockPlayer).drink();

        String failureString = "What?\n";

        // North command case-insensitivity
        testGame.doSomething("n");
        testGame.doSomething("N");
        verify(mockHouse, times(2)).moveNorth();
        // South command case-insensitivity
        testGame.doSomething("s");
        testGame.doSomething("S");
        verify(mockHouse, times(2)).moveSouth();
        // Look command case-insensitivity
        testGame.doSomething("l");
        testGame.doSomething("L");
        verify(mockHouse, times(2)).look(mockPlayer, null);
        // Inventory command case-insensitivity
        testGame.doSomething("i");
        testGame.doSomething("I");
        verify(mockPlayer, times(2)).showInventory();
        // Drink command case-insensitivity
        assertEquals(testGame.doSomething("d"), 1);
        assertEquals(testGame.doSomething("D"), 1);
        // Check for unknown command
        consoleOutput.reset();
        testGame.doSomething("Z");
        assertEquals(consoleOutput.toString(), failureString);
        // Checks that method is returning correct value
        assertEquals(testGame.doSomething("n"), 0);
        assertEquals(testGame.doSomething("N"), 0);
        assertEquals(testGame.doSomething("s"), 0);
        assertEquals(testGame.doSomething("S"), 0);
        assertEquals(testGame.doSomething("l"), 0);
        assertEquals(testGame.doSomething("L"), 0);
        assertEquals(testGame.doSomething("i"), 0);
        assertEquals(testGame.doSomething("I"), 0);
    }

    /**
     * Test for run().
     * Should return 0 if game is won or 1 if game is lost.
     */
    @Test
    public void testRun()
    {
        ByteArrayInputStream consoleInput;

        Player mockPlayer = mock(Player.class);
        House  mockHouse  = mock(House.class);
        Game   testGame   = new Game(mockPlayer, mockHouse);

        doReturn(true).when(mockPlayer).drink();
        consoleInput = new ByteArrayInputStream("D".getBytes());
        System.setIn(consoleInput);

        assertEquals(testGame.run(), 0); // Win condition

        doReturn(false).when(mockPlayer).drink();
        consoleInput = new ByteArrayInputStream("D".getBytes());
        System.setIn(consoleInput);

        assertEquals(testGame.run(), 1); // Loss condition
    }
}