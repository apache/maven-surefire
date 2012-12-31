package junit4;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public interface TopLevelInterfaceTest {
	public static class InnerTest {
		@Test
		public void testSomething() {
		}
	}
}
