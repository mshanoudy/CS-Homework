import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BotTest
{
    @Rule
    public ExpectedException thrown= ExpectedException.none();


    @Test
    public void testWait() throws Exception
    {
        Bot testBot = new Bot();

        thrown.expect(IllegalArgumentException.class);
        testBot.wait(-1);
    }

    @Test
    public void testKeyPress() throws Exception
    {
        Bot testBot = new Bot();

        thrown.expect(IllegalArgumentException.class);
        testBot.keyPress(0);
    }

    @Test
    public void testCursorMove() throws Exception
    {
        Bot mockBot = mock(Bot.class);
        Point point = new Point(0, 0);

        mockBot.cursorMove(point);
        verify(mockBot).cursorMove(point);
    }

    @Test
    public void testCursorPress() throws Exception
    {
        Bot mockBot = mock(Bot.class);

        mockBot.cursorPress();
        verify(mockBot).cursorPress();
    }

    @Test
    public void testCursorPress1() throws Exception
    {
        Bot mockBot = mock(Bot.class);
        Point point = new Point(0, 0);

        mockBot.cursorPress(point);
        verify(mockBot).cursorPress(point);
    }

    @Test
    public void testCursorPress2() throws Exception
    {
        Bot mockBot = mock(Bot.class);

        mockBot.cursorPress(0, 0);
        verify(mockBot).cursorPress(0, 0);
    }

    @Test
    public void testScreenShot() throws Exception
    {
        Bot testBot = new Bot();
        Robot robot = new Robot();
        Point point = new Point(0, 0);
        Dimension dimension = new Dimension(5, 5);
        Rectangle rectangle = new Rectangle(point, dimension);

        BufferedImage image_1 = testBot.screenShot(point, dimension);
        BufferedImage image_2 = robot.createScreenCapture(rectangle);

        assertEquals(image_1.getRaster().toString(), image_2.getRaster().toString());
    }
}