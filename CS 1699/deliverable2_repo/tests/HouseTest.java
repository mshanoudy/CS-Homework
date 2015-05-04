import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link House}.
 *
 * @author Mark Shanoudy
 */
public class HouseTest
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
     * Test for getCurrentRoomInfo().
     * Should return a String containing the description of the current room or return the Player to room 0 if
     * the Player moves out of House bounds.
     */
    @Test
    public void testGetCurrentRoomInfo()
    {
        Room[] mockRooms = new Room[2];
        mockRooms[0] = mock(Room.class);
        mockRooms[1] = mock(Room.class);

        doReturn("Room 0").when(mockRooms[0]).getDescription();
        doReturn("Room 1").when(mockRooms[1]).getDescription();
        String failureString = "You are in a magical land!  But you are returned to the beginning!";

        House testHouse = new House(mockRooms);

        // _currentRoom < 0
        testHouse.moveSouth();
        assertEquals(testHouse.getCurrentRoomInfo(), failureString);
        // Making sure returned to Room 0
        assertEquals(testHouse.getCurrentRoomInfo(), "Room 0");
        // Moving to Room 1
        testHouse.moveNorth();
        assertEquals(testHouse.getCurrentRoomInfo(), "Room 1");
        // _currentRoom >= _numRooms
        testHouse.moveNorth();
        assertEquals(testHouse.getCurrentRoomInfo(), failureString);
        // Making sure returned to Room 0
        assertEquals(testHouse.getCurrentRoomInfo(), "Room 0");
    }

    @Ignore
    @Test
    public void testMoveNorth()
    {
    }

    @Ignore
    @Test
    public void testMoveSouth()
    {
    }

    /**
     * Test for look().
     * Should check to see if current room contains an item. If there is an item present, look() calls the appropriate
     * method from Player.
     */
    @Test
    public void testLook()
    {
        Player mockPlayer = mock(Player.class);
        Room[] mockRooms = new Room[4];
        mockRooms[0] = mock(Room.class);
        mockRooms[1] = mock(Room.class);
        mockRooms[2] = mock(Room.class);
        mockRooms[3] = mock(Room.class);

        doReturn(true).when(mockRooms[0]).hasItem();
        doReturn(true).when(mockRooms[1]).hasItem();
        doReturn(true).when(mockRooms[2]).hasItem();
        doReturn(false).when(mockRooms[3]).hasItem();

        doReturn(true).when(mockRooms[0]).hasCoffee();
        doReturn(true).when(mockRooms[1]).hasCream();
        doReturn(true).when(mockRooms[2]).hasSugar();

        String failureString = "You don't see anything out of the ordinary.\r\n";

        House testHouse = new House(1);

        testHouse.look(mockPlayer, mockRooms[0]);   // Coffee
        verify(mockPlayer).getCoffee();
        testHouse.look(mockPlayer, mockRooms[1]);   // Cream
        verify(mockPlayer).getCream();
        testHouse.look(mockPlayer, mockRooms[2]);   // Sugar
        verify(mockPlayer).getSugar();
        consoleOutput.reset();
        testHouse.look(mockPlayer, mockRooms[3]);   // No item
        assertEquals(consoleOutput.toString(), failureString);
    }

    /**
     * Test for generateRooms().
     * Should return a Room[] with unique descriptions for each Room. Test is on an array of size 4 in order to ensure
     * all items are assigned to the House.
     */
    @Test
    public void testGenerateRooms()
    {
        Room[] mockRooms = new Room[4];
        mockRooms[0] = new Room(false, true, false, true, false);   // Cream and North door
        mockRooms[1] = new Room(false, false, false, true, true);   // No items, North and South doors
        mockRooms[2] = new Room(true, false, false, true, true);    // Coffee, North and South doors
        mockRooms[3] = new Room(false, false, true, false, true);   // Sugar, South door

        House testHouse = new House(1);
        Room[] testRooms = testHouse.generateRooms(4);
        // Room 0
        assertEquals(testRooms[0].hasCoffee(), mockRooms[0].hasCoffee());
        assertEquals(testRooms[0].hasCream(),  mockRooms[0].hasCream());
        assertEquals(testRooms[0].hasSugar(),  mockRooms[0].hasSugar());
        assertEquals(testRooms[0].northExit(), mockRooms[0].northExit());
        assertEquals(testRooms[0].southExit(), mockRooms[0].southExit());
        // Room 1
        assertEquals(testRooms[1].hasCoffee(), mockRooms[1].hasCoffee());
        assertEquals(testRooms[1].hasCream(),  mockRooms[1].hasCream());
        assertEquals(testRooms[1].hasSugar(),  mockRooms[1].hasSugar());
        assertEquals(testRooms[1].northExit(), mockRooms[1].northExit());
        assertEquals(testRooms[1].southExit(), mockRooms[1].southExit());
        // Room 2
        assertEquals(testRooms[2].hasCoffee(), mockRooms[2].hasCoffee());
        assertEquals(testRooms[2].hasCream(),  mockRooms[2].hasCream());
        assertEquals(testRooms[2].hasSugar(),  mockRooms[2].hasSugar());
        assertEquals(testRooms[2].northExit(), mockRooms[2].northExit());
        assertEquals(testRooms[2].southExit(), mockRooms[2].southExit());
        // Room 3
        assertEquals(testRooms[3].hasCoffee(), mockRooms[3].hasCoffee());
        assertEquals(testRooms[3].hasCream(),  mockRooms[3].hasCream());
        assertEquals(testRooms[3].hasSugar(),  mockRooms[3].hasSugar());
        assertEquals(testRooms[3].northExit(), mockRooms[3].northExit());
        assertEquals(testRooms[3].southExit(), mockRooms[3].southExit());
    }
}