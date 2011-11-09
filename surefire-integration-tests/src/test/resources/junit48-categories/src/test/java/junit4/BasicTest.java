package junit4;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;


public class BasicTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;
    static int catNoneCount = 0;

    @Test
    @Category(CategoryA.class)
    public void testInCategoryA()
    {
        System.out.println( "Ran testInCategoryA" );
        catACount++;
    }

    @Test
    @Category(CategoryB.class)
    public void testInCategoryB()
    {
        System.out.println( "Ran testInCategoryB" );
        catBCount++;
    }

    @Test
    @Category(CategoryC.class)
    public void testInCategoryC()
    {
        System.out.println( "Ran testInCategoryC" );
        catCCount++;
    }

    @Test
    public void testInNoCategory()
    {
        System.out.println( "Ran testInNoCategory" );
        catNoneCount++;
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        System.out.println("catA: " + catACount + "\n" +
            "catB: " + catBCount + "\n" +
            "catC: " + catCCount + "\n" +
            "catNone: " + catNoneCount);
    }
}
