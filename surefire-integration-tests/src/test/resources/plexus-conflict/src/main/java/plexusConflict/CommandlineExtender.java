package plexusConflict;

import org.codehaus.plexus.util.cli.Commandline;

/**
 * Conflict with latest version of plexus by using modified protected class.
 */
public class CommandlineExtender extends Commandline
{
    public CommandlineExtender() {
        // In 1.0.4, Commandline.envVars was a Vector; in 1.4.x, it's a Map.
        super.envVars.add("");
    }
}
