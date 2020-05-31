  ------
  Feature Matrix
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

Feature Matrix

    Not all features are supported for all test frameworks, and the following table gives a brief overview
    of support status:

*---------------------------------------------+-----------+-----------+------------+-----------+----------+----------------------+
|| <<Feature>>                                ||<<JUnit3>>||<<JUnit4>>||<<JUnit47>>||<<TestNG>>||<<POJO>> ||<<JUnit 5 Platform>> |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| groups/category/tags support                |     N      |    N     |      Y     |    Y      |  N       |  Y                   |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| security manager support                    |     Y      |    N     |      N     |    N      |  N       |  N                   |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| runOrder support                            |     Y      |    Y     |      Y     |    ?      |  Y       |  Y                   |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| run >1 individual test method in a class    |     N      |    Y     |      Y     |    Y      |  N       |  ?(*1)               |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| parallel support                            |     N      |    N     |      Y     |    Y      |  N       |  N(*2)               |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| custom run-listener                         |     N      |    Y     |      Y     |    Y      |  -       |  N                   |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| re-run count                                |     N      |    Y     |      Y     |    N      |  N       |  Y(*3)               |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| skip after failure count                    |     N      |    Y     |      Y     |    Y      |  N       |  N                   |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+
| Surefire Extensions                         |     Y      |    Y     |      Y     |    Y      |  Y       |  Y(*4)               |
*---------------------------------------------+------------+----------+------------+-----------+----------+----------------------+


    Legend: "Y" means supported, "N" means not supported. "?" means not tested.

    If you would like to implement support for
    a given provider with an "N" or a "?" (or create tests for it), you should create a patch and mark the issue as an
    improvement. If there is something wrong with an implementation marked with "Y" that is considered a bug.

   (*1) The JUnit 5 Platform supports running multiple individual test methods in a single class, but there are some
   corner cases that are not supported, yet: {{{https://github.com/junit-team/junit5/issues/1343}junit-team/junit5#1343}}
   and {{{https://github.com/junit-team/junit5/issues/1406}junit-team/junit5#1406)}}.

   (*2) The test are executed in parallel but the report supports only a sequence of test events, see the issue
   {{{https://issues.apache.org/jira/browse/SUREFIRE-1795}SUREFIRE-1795}}.

   (*3) Since 3.0.0-M4

   (*4) 3 extensions related to JUnit5 annotation <<<DisplayName>>>.
