package jiras.surefire955;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( SomeCategory.class )
public class CategorizedTest
{

    @Test
    public void a()
    {
        System.out.println( "CategorizedTest#a" );
    }

}
