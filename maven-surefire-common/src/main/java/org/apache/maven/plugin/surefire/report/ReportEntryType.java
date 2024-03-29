/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire.report;

/**
 * Type of an entry in the report
 *
 */
public enum ReportEntryType {
    ERROR("error", "flakyError", "rerunError"),
    FAILURE("failure", "flakyFailure", "rerunFailure"),
    SKIPPED("skipped", "", ""),
    SUCCESS("", "", "");

    private final String xmlTag;

    private final String flakyXmlTag;

    private final String rerunXmlTag;

    ReportEntryType(String xmlTag, String flakyXmlTag, String rerunXmlTag) {
        this.xmlTag = xmlTag;
        this.flakyXmlTag = flakyXmlTag;
        this.rerunXmlTag = rerunXmlTag;
    }

    public String getXmlTag() {
        return xmlTag;
    }

    public String getFlakyXmlTag() {
        return flakyXmlTag;
    }

    public String getRerunXmlTag() {
        return rerunXmlTag;
    }
}
