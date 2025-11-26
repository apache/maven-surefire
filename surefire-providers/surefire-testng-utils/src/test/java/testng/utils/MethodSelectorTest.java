/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package testng.utils;

import java.lang.reflect.Method;

import junit.framework.TestCase;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.testng.utils.MethodSelector;
import org.testng.IClass;
import org.testng.IRetryAnalyzer;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.internal.DefaultMethodSelectorContext;

/**
 *
 */
public class MethodSelectorTest extends TestCase {
    public void testInclusionOfMethodFromBaseClass() {
        MethodSelector selector = new MethodSelector();
        DefaultMethodSelectorContext context = new DefaultMethodSelectorContext();
        ITestNGMethod testngMethod = new FakeTestNGMethod(ChildClassSample.class, "baseClassMethodToBeIncluded");
        TestListResolver resolver =
                new TestListResolver(BaseClassSample.class.getName() + "#baseClassMethodToBeIncluded");
        MethodSelector.setTestListResolver(resolver);
        boolean include = selector.includeMethod(context, testngMethod, true);
        assertTrue(include);
    }

    public void testInclusionOfInheritedMethod() {
        MethodSelector selector = new MethodSelector();
        DefaultMethodSelectorContext context = new DefaultMethodSelectorContext();
        ITestNGMethod testngMethod = new FakeTestNGMethod(
                BaseClassSample.class, "baseClassMethodToBeIncluded", new FakeTestClass(ChildClassSample.class));
        TestListResolver resolver =
                new TestListResolver(ChildClassSample.class.getName() + "#baseClassMethodToBeIncluded");
        MethodSelector.setTestListResolver(resolver);
        boolean include = selector.includeMethod(context, testngMethod, true);
        assertTrue(include);
    }

    public void testNoInclusionOfMethodFromBaseClass() {
        MethodSelector selector = new MethodSelector();
        DefaultMethodSelectorContext context = new DefaultMethodSelectorContext();
        ITestNGMethod testngMethod = new FakeTestNGMethod(ChildClassSample.class, "baseClassMethodToBeIncluded");
        TestListResolver resolver = new TestListResolver(BaseClassSample.class.getName() + "#nonExistedMethod");
        MethodSelector.setTestListResolver(resolver);
        boolean include = selector.includeMethod(context, testngMethod, true);
        assertFalse(include);
    }

    public void testInclusionOfMethodFromSubClass() {
        MethodSelector selector = new MethodSelector();
        DefaultMethodSelectorContext context = new DefaultMethodSelectorContext();
        ITestNGMethod testngMethod = new FakeTestNGMethod(ChildClassSample.class, "subClassMethod");
        TestListResolver resolver = new TestListResolver(ChildClassSample.class.getName() + "#sub*");
        MethodSelector.setTestListResolver(resolver);
        boolean include = selector.includeMethod(context, testngMethod, true);
        assertTrue(include);
    }

    private static class FakeTestNGMethod implements ITestNGMethod {
        private final Class<?> clazz;
        private final String methodName;
        private final ITestClass testClass;

        FakeTestNGMethod(Class<?> clazz, String methodName) {
            this(clazz, methodName, new FakeTestClass(clazz));
        }

        FakeTestNGMethod(Class<?> clazz, String methodName, ITestClass testClass) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.testClass = testClass;
        }

        @Override
        public Class getRealClass() {
            return clazz;
        }

        @Override
        public ITestClass getTestClass() {
            return testClass;
        }

        @Override
        public void setTestClass(ITestClass iTestClass) {}

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public String getMethodName() {
            return methodName;
        }

        @Override
        public Object[] getInstances() {
            return new Object[0];
        }

        @Override
        public long[] getInstanceHashCodes() {
            return new long[0];
        }

        @Override
        public String[] getGroups() {
            return new String[0];
        }

        @Override
        public String[] getGroupsDependedUpon() {
            return new String[0];
        }

        @Override
        public String getMissingGroup() {
            return null;
        }

        @Override
        public void setMissingGroup(String s) {}

        @Override
        public String[] getBeforeGroups() {
            return new String[0];
        }

        @Override
        public String[] getAfterGroups() {
            return new String[0];
        }

        @Override
        public String[] getMethodsDependedUpon() {
            return new String[0];
        }

        @Override
        public void addMethodDependedUpon(String s) {}

        @Override
        public boolean isTest() {
            return false;
        }

        @Override
        public boolean isBeforeMethodConfiguration() {
            return false;
        }

        @Override
        public boolean isAfterMethodConfiguration() {
            return false;
        }

        @Override
        public boolean isBeforeClassConfiguration() {
            return false;
        }

        @Override
        public boolean isAfterClassConfiguration() {
            return false;
        }

        @Override
        public boolean isBeforeSuiteConfiguration() {
            return false;
        }

        @Override
        public boolean isAfterSuiteConfiguration() {
            return false;
        }

        @Override
        public boolean isBeforeTestConfiguration() {
            return false;
        }

        @Override
        public boolean isAfterTestConfiguration() {
            return false;
        }

        @Override
        public boolean isBeforeGroupsConfiguration() {
            return false;
        }

        @Override
        public boolean isAfterGroupsConfiguration() {
            return false;
        }

        @Override
        public long getTimeOut() {
            return 0;
        }

        @Override
        public int getInvocationCount() {
            return 0;
        }

        @Override
        public void setInvocationCount(int i) {}

        @Override
        public int getSuccessPercentage() {
            return 0;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public void setId(String s) {}

        @Override
        public long getDate() {
            return 0;
        }

        @Override
        public void setDate(long l) {}

        @Override
        public boolean canRunFromClass(IClass iClass) {
            return false;
        }

        @Override
        public boolean isAlwaysRun() {
            return false;
        }

        @Override
        public int getThreadPoolSize() {
            return 0;
        }

        @Override
        public void setThreadPoolSize(int i) {}

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public void incrementCurrentInvocationCount() {}

        @Override
        public int getCurrentInvocationCount() {
            return 0;
        }

        @Override
        public void setParameterInvocationCount(int i) {}

        @Override
        public int getParameterInvocationCount() {
            return 0;
        }

        @Override
        public ITestNGMethod clone() {
            try {
                return (ITestNGMethod) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public IRetryAnalyzer getRetryAnalyzer() {
            return null;
        }

        @Override
        public void setRetryAnalyzer(IRetryAnalyzer iRetryAnalyzer) {}

        @Override
        public boolean skipFailedInvocations() {
            return false;
        }

        @Override
        public void setSkipFailedInvocations(boolean b) {}

        @Override
        public long getInvocationTimeOut() {
            return 0;
        }

        @Override
        public boolean ignoreMissingDependencies() {
            return false;
        }

        @Override
        public void setIgnoreMissingDependencies(boolean b) {}

        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }

    private static class FakeTestClass implements ITestClass {

        private final Class<?> realClass;

        FakeTestClass(Class<?> realClass) {
            this.realClass = realClass;
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public String getTestName() {
            return "";
        }

        @Override
        public Class getRealClass() {
            return realClass;
        }

        @Override
        public Object[] getInstances(boolean reuse) {
            return new Object[0];
        }

        @Override
        public long[] getInstanceHashCodes() {
            return new long[0];
        }

        @Override
        public void addInstance(Object instance) {}

        @Override
        public int getInstanceCount() {
            return 0;
        }

        @Override
        public ITestNGMethod[] getTestMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getBeforeTestMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getAfterTestMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getBeforeClassMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getAfterClassMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getBeforeSuiteMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getAfterSuiteMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getBeforeTestConfigurationMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getAfterTestConfigurationMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getBeforeGroupsMethods() {
            return new ITestNGMethod[0];
        }

        @Override
        public ITestNGMethod[] getAfterGroupsMethods() {
            return new ITestNGMethod[0];
        }
    }
}
