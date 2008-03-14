package testng.pathWithSpaces;

import java.io.File;
import java.net.URISyntaxException;

import org.testng.annotations.Test;


public class TestNGSuiteTest {

	@Test
	public void loadTestResourceWithSpaces() throws URISyntaxException
	{
		new File( getClass().getResource( "/test.txt" ).toURI() );
	}
}