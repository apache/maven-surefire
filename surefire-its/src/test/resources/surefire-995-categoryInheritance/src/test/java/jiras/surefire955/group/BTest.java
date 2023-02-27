package jiras.surefire955.group;

import jiras.surefire955.group.marker.CategoryB;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category( CategoryB.class )
public class BTest
{

    @Test
    public void b()
    {
        System.out.println( "BTest#b" );
    }

}
