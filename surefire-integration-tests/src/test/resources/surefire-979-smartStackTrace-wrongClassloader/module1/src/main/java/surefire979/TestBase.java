package surefire979;


import org.apache.commons.io.input.AutoCloseInputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;


public class TestBase
{

    static {
        AutoCloseInputStream directoryWalker = new AutoCloseInputStream(new ByteArrayInputStream(new byte[200]));
    }

}
