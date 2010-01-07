package org.apache.maven.surefire.its;

public class IntegrationTestSuiteResults
{
    private int total, errors, failures, skipped;

    public IntegrationTestSuiteResults( int total, int errors, int failures, int skipped )
    {
        this.total = total;
        this.errors = errors;
        this.failures = failures;
        this.skipped = skipped;
    }

    public int getTotal()
    {
        return total;
    }

    public void setTotal( int total )
    {
        this.total = total;
    }

    public int getErrors()
    {
        return errors;
    }

    public void setErrors( int errors )
    {
        this.errors = errors;
    }

    public int getFailures()
    {
        return failures;
    }

    public void setFailures( int failures )
    {
        this.failures = failures;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public void setSkipped( int skipped )
    {
        this.skipped = skipped;
    }
    
}
