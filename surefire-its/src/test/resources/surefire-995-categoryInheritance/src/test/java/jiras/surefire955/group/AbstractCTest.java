package jiras.surefire955.group;

import jiras.surefire955.group.marker.CategoryC;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category( CategoryC.class )
public abstract class AbstractCTest
{

    @Test
    public void pc()
    {
        System.out.println( "AbstractCTest#pc" );
    }

}
