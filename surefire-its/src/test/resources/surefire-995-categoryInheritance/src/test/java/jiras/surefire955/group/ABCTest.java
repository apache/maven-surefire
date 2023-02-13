package jiras.surefire955.group;

import jiras.surefire955.group.marker.CategoryA;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( CategoryA.class )
public class ABCTest extends jiras.surefire955.group.AbstractBCTest
{

    @Test
    public void abc()
    {
        System.out.println( "ABCTest#abc" );
    }

}
