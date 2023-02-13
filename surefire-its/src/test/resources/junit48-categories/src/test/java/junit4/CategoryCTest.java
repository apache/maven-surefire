package junit4;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;


@Category(CategoryC.class)
public class CategoryCTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;
    static int catNoneCount = 0;

    @Test
    public void testInCategoryA()
    {
        catACount++;
    }

    @Test
    @Category(CategoryB.class)
    public void testInCategoryB()
    {
        catBCount++;
    }

    @Test
    @Category(CategoryC.class)
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
        System.out.println("mA: " + catACount + "\n" +
            "mB: " + catBCount + "\n" +
            "mC: " + catCCount + "\n" +
            "CatNone: " + catNoneCount);
    }
}
