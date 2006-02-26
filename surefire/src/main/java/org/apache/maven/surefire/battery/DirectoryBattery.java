package org.apache.maven.surefire.battery;

/*
 * Copyright 2001-2005 The Codehaus.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.StringUtils;

public class DirectoryBattery extends AbstractBattery {

    private static final String FS = System.getProperty("file.separator");

    private File basedir;

    private List includes;

    private List excludes;

    public DirectoryBattery(File basedir, ArrayList includes, ArrayList excludes)
        throws Exception
    {
        this.basedir = basedir;

        this.includes = includes;

        this.excludes = excludes;

        discoverBatteryClassNames();
    }
    
    public void discoverBatteryClassNames()
        throws Exception
    {
        String[] tests = collectTests(basedir, includes, excludes);

        if (tests == null) { return; }

        for(int i = 0; i < tests.length; i++) {
            String s = tests[i];

            s = s.substring(0, s.indexOf("."));

            s = s.replace(FS.charAt(0), ".".charAt(0));

            addSubBatteryClassName(s);
        }
    }

    public String[] collectTests(File basedir, List includes, List excludes)
        throws Exception
    {
        if (!basedir.exists()) { return null; }

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(basedir);

        if (includes != null) {
            String[] incs = new String[includes.size()];

            for(int i = 0; i < incs.length; i++) {
                incs[i] = StringUtils.replace((String)includes.get(i), "java",
                        "class");

            }

            scanner.setIncludes(incs);
        }

        if (excludes != null) {
            String[] excls = new String[excludes.size()];

            for(int i = 0; i < excls.length; i++) {
                excls[i] = StringUtils.replace((String)excludes.get(i), "java",
                        "class");
            }

            scanner.setExcludes(excls);
        }

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
