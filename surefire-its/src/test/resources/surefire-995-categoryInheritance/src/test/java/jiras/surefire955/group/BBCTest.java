package jiras.surefire955.group;

import jiras.surefire955.group.marker.CategoryB;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( CategoryB.class )
public class BBCTest extends jiras.surefire955.group.AbstractBCTest
{

    @Test
    public void bbc()
    {
        System.out.println( "BBCTest#bbc" );
    }

}
