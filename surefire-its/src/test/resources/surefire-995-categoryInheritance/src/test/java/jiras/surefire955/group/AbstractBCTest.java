package jiras.surefire955.group;

import jiras.surefire955.group.marker.CategoryB;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category( CategoryB.class )
public abstract class AbstractBCTest extends jiras.surefire955.group.AbstractCTest
{

    @Test
    public void pb()
    {
        System.out.println( "AbstractBCTest#pb" );
    }

}
