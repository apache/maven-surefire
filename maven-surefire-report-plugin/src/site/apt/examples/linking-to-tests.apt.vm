  ------
  Linking to Tests
  ------
  Sean Griffin
  ------
  May 2015
  ------

~~ Copyright 2006 The Apache Software Foundation.
~~
~~ Licensed under the Apache License, Version 2.0 (the "License");
~~ you may not use this file except in compliance with the License.
~~ You may obtain a copy of the License at
~~
~~      http://www.apache.org/licenses/LICENSE-2.0
~~
~~ Unless required by applicable law or agreed to in writing, software
~~ distributed under the License is distributed on an "AS IS" BASIS,
~~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
~~ See the License for the specific language governing permissions and
~~ limitations under the License.

~~ NOTE: For help with the syntax of this file, see:
~~ http://maven.apache.org/doxia/references/apt-format.html

Linking to Tests

  Anchors surround the names of each test suite and test case in the report so
  that you can remotely link to them.

  The format of the anchor names for test suites is as follows:

+----+
<package><suiteName>
+----+

  Notice that there is no "." between the package name and the suite name.

  The format of the anchor names for test cases is as follows:

+----+
TC_<package>.<suiteName>.<testName>
+----+

  To illustrate with an example, suppose the following:

  * Package name: org.foo

  * Test suite name: FooTest

  * Test names: testFooWithBar, testFooWithBaz

  []

  The anchor name for the test suite would be "org.fooFooTest" and the anchor
  names for the test cases would be "TC_org.foo.FooTest.testFooWithBar" and
  "TC_org.foo.FooTest.testFooWithBaz".