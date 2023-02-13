package jiras.surefire955.group;

import jiras.surefire955.group.marker.CategoryA;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( CategoryA.class )
public class ATest
{

    @Test
    public void a()
    {
        System.out.println( "ATest#a" );
    }

}
