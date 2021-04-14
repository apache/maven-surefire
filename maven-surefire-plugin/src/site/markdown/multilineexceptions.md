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

Multi-line exception messages
=============================

Surefire 2.19 introduced special handling for multi-lined exception messages in order to facilitate vertical alignment. For example,

    java.lang.IllegalArgumentException: My Couch
       |
       May not contain whitespace
    
becomes:

      java.lang.IllegalArgumentException:
      The Couch
         |
         May not contain whitespace

The plugin supports Groovy assertion output.
For more information see the issue https://issues.apache.org/jira/browse/SUREFIRE-986.
