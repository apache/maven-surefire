package junit4;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;


@Category(CategoryC.class)
public class CategoryCTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;

    @Rule
    public TestName testName = new TestName();
    
    @Before
    public void testName()
    {
        System.out.println( "Running " + getClass().getName() + "." + testName.getMethodName() );
    }
    
    @Test
    @Category( CategoryA.class )
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
    @Category({CategoryA.class, CategoryB.class})
    public void testInCategoriesAB()
    {
        catACount++;
        catBCount++;
    }

    @Test
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
