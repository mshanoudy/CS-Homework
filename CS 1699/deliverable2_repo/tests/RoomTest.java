import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

/**
 * Tests for {@link Room}.
 *
 * @author Mark Shanoudy
 */
public class RoomTest
{
    @Test
    public void testHasItem()
    {
        Room testRoom;
                        // Coffee, cream, sugar, North, South
        testRoom = new Room(false, false, false, true, false);
        assertFalse(testRoom.hasItem());
        testRoom = new Room(true, false, false, true, false);
        assertTrue(testRoom.hasItem());
        testRoom = new Room(false, true, false, true, false);
        assertTrue(testRoom.hasItem());
        testRoom = new Room(false, false, true, true, false);
        assertTrue(testRoom.hasItem());
        testRoom = new Room(true, true, true, true, false);
        assertTrue(testRoom.hasItem());
    }

    @Test
    public void testHasSugar()
    {
        Room testRoom;
                        // Coffee, cream, sugar, North, South
        testRoom = new Room(false, false, true, true, false);
        assertTrue(testRoom.hasSugar());
        testRoom = new Room(false, false, false, true, false);
        assertFalse(testRoom.hasSugar());
    }

    @Test
    public void testHasCream()
    {
        Room testRoom;
                        // Coffee, cream, sugar, North, South
        testRoom = new Room(false, true, false, true, false);
        assertTrue(testRoom.hasCream());
        testRoom = new Room(false, false, false, true, false);
        assertFalse(testRoom.hasCream());
    }

    @Test
    public void testHasCoffee()
    {
        Room testRoom;
                        // Coffee, cream, sugar, North, South
        testRoom = new Room(true, false, false, true, false);
        assertTrue(testRoom.hasCoffee());
        testRoom = new Room(false, false, false, true, false);
        assertFalse(testRoom.hasCoffee());
    }

    @Test
    public void testNorthExit()
    {
        Room testRoom;
                        // Coffee, cream, sugar, North, South
        testRoom = new Room(false, false, false, true, false);
        assertTrue(testRoom.northExit());
        testRoom = new Room(false, false, false, false, false);
        assertFalse(testRoom.northExit());
    }

    @Test
    public void testSouthExit()
    {
        Room testRoom;
                        // Coffee, cream, sugar, North, South
        testRoom = new Room(false, false, false, false, true);
        assertTrue(testRoom.southExit());
        testRoom = new Room(false, false, false, false, false);
        assertFalse(testRoom.southExit());
    }

    @Ignore
    @Test
    public void testGetDescription()
    {
        String[] expectedDescriptions = new String[2];
        expectedDescriptions[0] =   "\nYou see a Small room.\n"+
                                    "It has a Quaint kettle.\n" +
                                    "A Magenta door leads North.\n";
        expectedDescriptions[1] =   "\nYou see a Funny room.\n" +
                                    "It has a Sad record player.\n" +
                                    "A Beige door leads South.\n";

        Room[] testRooms = new Room[2];
        testRooms[0] = new Room(false, true, false, true, false);
        testRooms[1] = new Room(false, false, true, false, true);

        assertEquals(testRooms[0].getDescription(), expectedDescriptions[0]);
        assertEquals(testRooms[1].getDescription(), expectedDescriptions[1]);
    }
}