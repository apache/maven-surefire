package org.apache.maven.surefire.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TheTest
{
    @Test
    public void checkSuccessCLIParam()
    {
        assertThat( Boolean.getBoolean( "success" ), equalTo( true ) );
    }

}
