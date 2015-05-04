import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link CoffeeMaker}.
 *
 * @author Mark Shanoudy
 */
public class CoffeeMakerTest
{
    /**
     * runArgs should return 0.
     */
    @Test
    public void testRunArgs()
    {
        CoffeeMaker testCoffeeMaker = new CoffeeMaker();
        assertEquals(testCoffeeMaker.runArgs("test"), 0);
    }
}