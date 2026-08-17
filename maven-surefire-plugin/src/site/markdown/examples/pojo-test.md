  ------
  Using POJO Tests
  ------
  Christian Gruber
  ------
  May 2008
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

Using POJO Tests

  POJO tests look very much like JUnit or TestNG tests, though they do not
  require dependencies on these artifacts.  A test class should be named
  <<<**/*Test>>> and should contain <<<test*>>> methods which will each be 
  executed by Surefire.
  
  Validating assertions can be done using the JDK 1.4 <<<assert>>> keyword.
  Simultaneous test execution is not possible with POJO tests.
  
  Fixtures can be setup before and after each <<<test*>>> method by implementing
  a set-up and a tear-down methods.  These methods must match the following signatures
  to be recognized and executed before and after each test method:
     
+---+
public void setUp();
public void tearDown();
+---+

  These fixture methods can also throw any exception and will still be valid.

