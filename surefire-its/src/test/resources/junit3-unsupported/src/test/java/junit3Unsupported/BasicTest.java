package junit3Unsupported;

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import junit.framework.TestCase;

public class BasicTest extends TestCase
{

    public void testEnvVar()
    {
        assertEquals(1, 1);
    }


}
