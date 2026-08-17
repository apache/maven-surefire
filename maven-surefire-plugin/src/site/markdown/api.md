  ------
  Provider API
  ------
  Kristian Rosenvold
  ------
  2010-12-09
  ------
  
~~ Licensed to the Apache Software Foundation (ASF) under one
~~ or more contributor license agreements.  See the NOTICE file
~~ distributed with this work for additional information
~~ regarding copyright ownership.  The ASF licenses this file
~~ to you under the Apache License, Version 2.0 (the
~~ "License"); you may not use this file except in compliance
~~ with the License.  You may obtain a copy of the License at
~~
~~   http://www.apache.org/licenses/LICENSE-2.0
~~
~~ Unless required by applicable law or agreed to in writing,
~~ software distributed under the License is distributed on an
~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~~ KIND, either express or implied.  See the License for the
~~ specific language governing permissions and limitations
~~ under the License.

~~ NOTE: For help with the syntax of this file, see:
~~ http://maven.apache.org/doxia/references/apt-format.html


Maven Surefire Provider API

  As of version 2.7 of Surefire, there is a proposed public API available for
  external providers to use Surefire features.

  The key features of Surefire are forking, reporting and directory/classpath scanning.
  The remaining features are implemented in the providers.

  Please note that this API is still subject to change until otherwise declared, even in minor revisions.
  Such changes will mostly happen to facilitate needs of new providers.

* Requirements for a Provider

  There are three things any provider must fulfill:

  * A provider must implement the <<<org.apache.maven.surefire.providerapi.SurefireProvider>>> interface.

  * A provider contains a <<<META-INF/services>>> file entry named <<<org.apache.maven.surefire.providerapi.SurefireProvider>>>
  (as per {{{http://download.oracle.com/javase/6/docs/api/java/util/ServiceLoader.html}ServiceLoader}}). This file
  contains the fully qualified name of the actual provider class.

  * The actual provider class contains a one-arg constructor that accepts an instance of
  <<<org.apache.maven.surefire.providerapi.ProviderParameters>>>. This interface delivers all Surefire features
  to the provider implementation. Please see the javadoc of this interface for options.

  []

  There are four well-known providers within Surefire that are also implemented this way, so
  examples can be found by looking at the Surefire source code itself. <<<surefire-junit47>>> is
  the showcase implementation.

  The javadoc on the intefaces mentioned in this article should otherwise be sufficient to write a provider.
  Providers are added as dependencies to the Surefire and Failsafe plugins.

** API Changes for 2.11

    Prior to 2.11, the provider would do

+---+
    TestsToRun scanned = directoryScanner.locateTestClasses( testClassLoader, scannerFilter );
+---+

    and the classes would arrive in sorted order. In 2.11, an additional step must be implemented by the provider;

+---+
    TestsToRun scanned = directoryScanner.locateTestClasses( testClassLoader, scannerFilter );
    return providerParameters.getRunOrderCalculator().orderTestClasses(  scanned );
+---+

** API changes for 2.12.2:

   Prior to this version, the provider would do

+---+
    directoryScanner = booterParameters.getDirectoryScanner();
    final TestsToRun scanResult = directoryScanner.locateTestClasses( testClassLoader, testChecker );
+---+

   In this version <<<ProviderParameters#getDirectoryScanner>>> has been deprecated, and it *will* be removed
   for the next major version. Instead, use

+---+
    scanResult = booterParameters.getScanResult();
    final TestsToRun testsToRun = scanResult.applyFilter(testChecker, testClassLoader );
+---+
