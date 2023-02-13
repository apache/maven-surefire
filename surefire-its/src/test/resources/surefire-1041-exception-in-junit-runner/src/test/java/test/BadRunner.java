package test;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 *
 * @author Dan Fabulich
 *
 */
public class BadRunner extends BlockJUnit4ClassRunner{

	public BadRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

	@Override
	public void run(RunNotifier notifier) {
		String x = null;
		if (false) x = "";
		x.toString();
	}
}
