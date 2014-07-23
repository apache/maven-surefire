package junit4;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.RandomStringUtils;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class DmKernelTestRunner extends BlockJUnit4ClassRunner {

    public DmKernelTestRunner(Class<?> klass) throws InitializationError
    {
        super(klass);
    }

    @Override
    public void run(RunNotifier notifier)
    {
        try
        {
            launchOsgi();
            Object targetBundleContext = null;
            Class<?> realTestClass = createOsgiTestClass(targetBundleContext);
            BlockJUnit4ClassRunner realRunner = new BlockJUnit4ClassRunner(realTestClass);
            realRunner.run(notifier);
        } catch (Throwable e)
        {
            notifier.fireTestFailure(new Failure(getDescription(), e));
        } finally
        {
            stopOsgi();
        }
    }

    private void launchOsgi() throws InterruptedException
    {
        System.out.println("Launching OSGi framework for " + getTestClass().getJavaClass().getName());
        generateOsgiOutput(1100000);
        System.out.println("Virgo ready for " + getTestClass().getJavaClass().getName());
    }

    private void stopOsgi()
    {
        System.out.println("Shutting down OSGi framework for " + getTestClass().getJavaClass().getName());
        generateOsgiOutput(1000);
    }

    private Class<?> createOsgiTestClass(Object targetBundleContext)
    {
        return getTestClass().getJavaClass();
    }

    private void generateOsgiOutput(int n)
    {
        for (int i = 0; i < n / 100; i++)
        {
            System.out.println(RandomStringUtils.randomAlphabetic(100));
        }
    }

}
