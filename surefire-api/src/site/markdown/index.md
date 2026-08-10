---
title: Surefire API Design
author:
  - Brett Porter
date: 2007-03-03
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Surefire API

## Definitions

<table class="table table-bordered table-striped">
<tr><th>Term</th><th>Definition</th></tr>
<tr><td style="text-align: left;">test method</td><td style="text-align: left;">Individual test method within a class</td></tr>
<tr><td style="text-align: left;">test</td><td style="text-align: left;">1..N test methods in 1 or more classes.</td></tr>
<tr><td style="text-align: left;">suite</td><td style="text-align: left;">1..N tests.</td></tr>
<tr><td style="text-align: left;">group</td><td style="text-align: left;">A named subset of test methods within a test.</td></tr>
</table>

How each definition is applied depends on the provider, and the test suite being used.

- Directory test suite: this constructs a single suite from a directory file set. Each discovered class is treated as a test.
- TestNG XML test suite: this constructs a single suite from a `testng.xml` file. The definitions inside the file will match those above.

See [Surefire Providers](../surefire-providers/index.html) for more information on specific providers.

<!--TODO: fix up URLs, move some to providers/javadoc.-->
