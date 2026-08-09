---
title: Introduction
author:
  - Allan Ramirez
date: July 2006
---

<!-- Copyright 2006 The Apache Software Foundation.-->
<!---->
<!-- Licensed under the Apache License, Version 2.0 (the "License");-->
<!-- you may not use this file except in compliance with the License.-->
<!-- You may obtain a copy of the License at-->
<!---->
<!--      http://www.apache.org/licenses/LICENSE-2.0-->
<!---->
<!-- Unless required by applicable law or agreed to in writing, software-->
<!-- distributed under the License is distributed on an "AS IS" BASIS,-->
<!-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.-->
<!-- See the License for the specific language governing permissions and-->
<!-- limitations under the License.-->
# Maven Surefire Report Plugin

The Surefire Report Plugin parses the generated `TEST-*.xml` files under `${basedir}/target/surefire-reports` and renders them using DOXIA, which creates the web interface version of the test results.

## Goals Overview

The Surefire Report Plugin has three goals:

- [surefire-report:report](./report-mojo.html) Generates the test results report into HTML format.
- [surefire-report:report-only](./report-only-mojo.html) This goal does not run the tests, it only builds the report. It is provided as a work around for [SUREFIRE-257](https://issues.apache.org/jira/browse/SUREFIRE-257)
- [surefire-report:failsafe-report-only](./failsafe-report-only-mojo.html) This goal does not run the tests, it only builds the IT reports. See [SUREFIRE-257](https://issues.apache.org/jira/browse/SUREFIRE-257)

_Note:_ As of version 2.8 this plugin requires Maven Site Plugin 2.1 or newer to work properly. Version 2.7.2 and older are still compatible with newer Surefire versions, so mixing is possible.

## Usage

General instructions on how to use the Surefire Report Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples listed below. Additionally, users can contribute to the [GitHub project](https://github.com/apache/maven-surefire).

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can file a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](http://maven.apache.org/guides/development/guide-helping.html).

## Examples

The following examples show how to use the Surefire Report Plugin in more advanced use cases:

- [Showing Only Failed Tests](./examples/show-failures.html)
- [Changing the Report Name](./examples/changing-report-name.html)
- [Configuring the Output Location of the Report](./examples/report-custom-location.html)
- [Source Code Cross Reference](./examples/cross-referencing.html)
