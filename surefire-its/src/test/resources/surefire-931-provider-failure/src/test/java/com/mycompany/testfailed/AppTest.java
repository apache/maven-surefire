package com.mycompany.testfailed;

import junit.framework.TestCase;
import org.testng.annotations.Test;


/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    @Test(groups = "deleteLocation", dependsOnGroups =
    {
        "postLocation", "getLocation"
    })
    public void removeNonExistentLocation() {}
}
