package testng;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class CategoryCTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;

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
    public void testInCategoriesAB()
    {
        System.out.println( getClass().getSimpleName() + ".testInCategoriesAB()" );
        catACount++;
        catBCount++;
    }

    @Test( groups="CategoryC" )
    public void testInCategoryC()
    {
        catCCount++;
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        System.out.println("mA: " + catACount + "\n" +
            "mB: " + catBCount + "\n" +
            "mC: " + catCCount );
    }
}
