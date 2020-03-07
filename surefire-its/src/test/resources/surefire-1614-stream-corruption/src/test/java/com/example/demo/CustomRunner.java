package com.example.demo;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

public class CustomRunner
        extends BlockJUnit4ClassRunner
{

    public CustomRunner( Class<?> klass ) throws InitializationError
    {
        super( klass );
    }

    @Override
    protected TestClass createTestClass( Class<?> testClass )
    {
        System.out.println( "Creating test class" );
        return super.createTestClass( testClass );
    }
}
