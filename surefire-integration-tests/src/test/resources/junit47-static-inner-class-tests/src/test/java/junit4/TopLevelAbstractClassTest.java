package junit4;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public abstract class TopLevelAbstractClassTest {
	public static class InnerTest {
		@Test
		public void testSomething() {
		}
	}
}
