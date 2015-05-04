import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LividFarmTest
{
    @Test
    public void testStart() throws Exception
    {
        LividFarm mockLividFarm = mock(LividFarm.class);

        mockLividFarm.start();
        verify(mockLividFarm).start();
    }
}