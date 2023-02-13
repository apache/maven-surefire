package junit4;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;


public class BasicTest
{
    static int catACount = 0;
    static int catBCount = 0;
    static int catCCount = 0;
    static int catNoneCount = 0;
    
    @Rule
    public TestName testName = new TestName();
    
    @Before
    public void testName()
    {
        System.out.println( "Running " + getClass().getName() + "." + testName.getMethodName() );
    }
    
    @Test
    @Category(CategoryA.class)
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
    public void testInCategoryAB()
    {
        catACount++;
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
        System.out.println("catA: " + catACount + "\n" +
            "catB: " + catBCount + "\n" +
            "catC: " + catCCount + "\n" +
            "catNone: " + catNoneCount);
    }
}
