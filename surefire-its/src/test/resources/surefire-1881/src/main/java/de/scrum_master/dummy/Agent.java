package de.scrum_master.dummy;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent
{
    public static volatile Instrumentation INSTRUMENTATION;

    public static void premain( String commandLineOptions, Instrumentation instrumentation )
    {
        INSTRUMENTATION = instrumentation;
        // These console outputs still work in Surefire/Failsafe freeze when using option
        //   <forkNode implementation="org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory"/>
        System.out.println( "[Agent OUT] Hello world!" );
        System.err.println( "[Agent ERR] Uh-oh!" );
        instrumentation.addTransformer( new DummyTransformer(), true );
    }

    public static class DummyTransformer implements ClassFileTransformer
    {
        @Override
        public byte[] transform( ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer )
        {
      /*
      FIXME: These console outputs make Surefire/Failsafe freeze when using option
        <forkNode implementation="org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory"/>

      FIXME: But when *not* using that option, they mess up console output like this (some lines in between deleted):
        [Transformer OUT] cl[Transformer ERR] className = sun/reflect/generics/tree/TypeArgument, class file size = 173
        assName = sun/reflect/generics/tree/SimpleClassTypeSignature, loader = null
        [Transformer OUT] className = java/lang/annotation/Retention[Transformer ERR] className = sun/reflect/generics/visitor/Reifier, class file size = 8341
        Policy, loader = null
        [Transformer OUT] className = org/junit/runner/notification/SynchronizedRunListener, loader[Transformer ERR] className = com/sun/proxy/$Proxy0, class file size = 2268
         = jdk.internal.loader.ClassLoaders$AppClassLoader@5451c3a8
        [T[Transformer ERR] className = org/junit/runners/BlockJUnit4ClassRunner, class file size = 14072
        ransformer OUT] className = org/junit/runners/ParentRunner, loader = jdk.internal.loader.ClassLoaders$AppClassLoader@5451c3a8
        Transformer ERR] className = org/junit/validator/AnnotationsValidator$FieldValidator, class file size = 2179
        [[Transformer OUT] className = org/junit/validator/AnnotationsValidator$FieldValidator, loader = jdk.internal.loader.ClassLoaders$AppClassLoader@5451c3a8
      */
            System.out.println( "[Transformer OUT] className = " + className + ", loader = " + loader );
            System.err.println( "[Transformer ERR] className = " + className + ", class file size = "
                + classfileBuffer.length );
            return null;
        }
    }
}

