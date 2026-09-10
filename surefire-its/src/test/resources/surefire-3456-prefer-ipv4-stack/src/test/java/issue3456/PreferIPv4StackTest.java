package issue3456;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that {@code java.net.preferIPv4Stack}, configured through {@code systemPropertyVariables}, is applied
 * early enough in the fork to actually switch the JVM to the IPv4 stack.
 * <p>
 * When the property arrives after the native "net" library has been loaded, the JVM keeps the dual/IPv6 stack and
 * {@link NetworkInterface} still reports IPv6 addresses. On a host without any IPv6 address the second assertion is
 * satisfied trivially; the first one still guards the plain propagation of the property.
 */
public class PreferIPv4StackTest {

    @Test
    public void preferIPv4StackIsEffective() throws Exception {
        assertEquals(
                "property not propagated to the fork", "true", System.getProperty("java.net.preferIPv4Stack"));

        List<String> ipv6Addresses = new ArrayList<>();
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress address : Collections.list(ni.getInetAddresses())) {
                if (address instanceof Inet6Address) {
                    ipv6Addresses.add(ni.getName() + " -> " + address);
                }
            }
        }

        assertTrue(
                "the fork still runs on the IPv6 stack, java.net.preferIPv4Stack was applied too late: "
                        + ipv6Addresses,
                ipv6Addresses.isEmpty());
    }
}
