package testng;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;


public class BasicTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;
    static int catNoneCount = 0;
    
    @Test( groups = "CategoryA" )
    public void testInCategoryA()
    {
        catACount++;
    }

    @Test( groups = "CategoryB" )
    public void testInCategoryB()
    {
        catBCount++;
    }

    @Test( groups = { "CategoryA", "CategoryB" } )
    public void testInCategoryAB()
    {
        System.out.println( getClass().getSimpleName() + ".testInCategoriesAB()" );
        catACount++;
        catBCount++;
    }

    @Test( groups = "CategoryC" )
    public void testInCategoryC()
    {
        catCCount++;
    }

    @Test
    public void testInNoCategory()
    {
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
