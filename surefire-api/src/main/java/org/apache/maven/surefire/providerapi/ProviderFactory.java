package org.apache.maven.surefire.providerapi;

/**
 * @author Kristian Rosenvold
 */
public interface ProviderFactory
{
    SurefireProvider createProvider();
}
