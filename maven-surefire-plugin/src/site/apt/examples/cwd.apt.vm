 ------
 Special characters in directories
 ------
 Tibor Digana
 ------
 2019-05-10
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


Working Directory with special characters

 You can use a colon (<<<:>>>) in the cwd on Linux but you have to turn the path separator from colon to
 some other character, see {{{https://issues.apache.org/jira/browse/SUREFIRE-1617}SUREFIRE-1617}}

 You should change the classpath separator (we do not guarantee that your application and test would run seamlessly)
 in order to avoid collisions with paths within the forked JVM:

+---+
<argLine>-Dpath.separator=;</argLine>
+---+

 For more information about the configuration parameter <<<workingDirectory>>>
 see the chapter {{{./fork-options-and-parallel-execution.html#Forked_Test_Execution}Forked Test Execution}}.


Local Repository with special characters

 The plugin supports special characters (i.e. a colon (<<<:>>>)) in the path of Maven local repository.
 The compiler does not support it and therefore the sources have to be compiled by another command in advance.

