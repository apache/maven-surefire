import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

import org.mortbay.jetty.Server;


public class WebAppTest extends TestCase {
    private Server server = null;
    
    public void setUp() throws Exception {
        System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
        
        server = new Server();
        String testPort = ":18080";
        server.addListener(testPort);
        server.addWebApplication("127.0.0.1", "/webapp", "target/webapp");
        
        server.start();
    }
    
    public void testBlah() throws Exception {
        URL url = new URL("http://127.0.0.1:18080/webapp/index.jsp");
        InputStream stream = url.openStream();
        StringBuffer sb = new StringBuffer();
        for (int i = stream.read(); i != -1; i = stream.read()) {
            sb.append((char)i);
        }
        String value = sb.toString();
        assertTrue(value, value.contains("Hello"));
    }
    
    public void tearDown() throws Exception {
        if (server != null) server.stop();
    }
}
