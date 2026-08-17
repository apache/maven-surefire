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

 -----
 Surefire API Design
 -----
 Brett Porter
 -----
 2007-03-03
 -----

Surefire API

* Definitions

*-------------+-----------------------------------------------+
| test method | Individual test method within a class         |
*-------------+-----------------------------------------------+
| test        | 1..N test methods in 1 or more classes.       |
*-------------+-----------------------------------------------+
| suite       | 1..N tests.                                   |
*-------------+-----------------------------------------------+
| group       | A named subset of test methods within a test. |
*-------------+-----------------------------------------------+

  How each definition is applied depends on the provider, and the test suite being used.

  * Directory test suite: this constructs a single suite from a directory file set. Each discovered class is treated as a test.

  * TestNG XML test suite: this constructs a single suite from a <<<testng.xml>>> file. The definitions inside the file will match those above.

  * JUnit 3.x: Groups are not supported.

  []

  See {{{../surefire-providers/index.html}Surefire Providers}} for more information on specific providers.

~~TODO: fix up URLs, move some to providers/javadoc.
