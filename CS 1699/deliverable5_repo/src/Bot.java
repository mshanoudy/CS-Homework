import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;

public class Bot
{
    private static final int MAX_ROBOT_DELAY    = 60000;    // Max value accepted by java.awt.Robot.delay()
    private static final int CURSOR_PRESS_DELAY = 500;      // Pause for cursor press to register
    private static final int KEY_PRESS_DELAY    = 500;      // Pause for key press to register
    private static final int SCREENSHOT_DELAY   = 150;      // Pause for info box to appear

    private Robot robot;

    /**
     * Constructor
     * @throws Exception
     */
    public Bot() throws Exception
    {
        this.robot = new Robot();
    }

    /**
     * Bot waits given amount of time
     * @param ms milliseconds to wait
     */
    public void wait(int ms)
    {
        if (ms > MAX_ROBOT_DELAY)
        {
            int quotient  = ms / MAX_ROBOT_DELAY;
            int remainder = ms % MAX_ROBOT_DELAY;

            for (int x = 0; x < quotient; x++)
                robot.delay(MAX_ROBOT_DELAY);
            robot.delay(remainder);
        }
        else
            robot.delay(ms);
    }

    /**
     * Bot presses keyboard
     * @param key key to press
     */
    public void keyPress(int key)
    {
        robot.keyPress(key);
        wait(KEY_PRESS_DELAY);
        robot.keyRelease(key);
    }

    /**
     * Bot moves cursor
     * @param point coordinate to move cursor
     */
    public void cursorMove(Point point)
    {
        robot.mouseMove((int) point.getX(), (int) point.getY());
    }

    /**
     * Bot presses cursor
     */
    public void cursorPress()
    {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        wait(CURSOR_PRESS_DELAY);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Bot presses cursor
     * @param point coordinate to move cursor
     */
    public void cursorPress(Point point)
    {
        cursorMove(point);
        cursorPress();
    }

    /**
     * Bot presses cursor
     * @param x x coordinate
     * @param y y coordinate
     */
    public void cursorPress(int x, int y)
    {
        cursorPress(new Point(x, y));
    }

    /**
     * Bot takes screenshot
     * @param point top-left coordinate of screenshot
     * @param dimension dimension of screenshot
     * @return screenshot
     */
    public BufferedImage screenShot(Point point, Dimension dimension)
    {
        wait(SCREENSHOT_DELAY);
        return robot.createScreenCapture(new Rectangle(point, dimension));
    }
}
