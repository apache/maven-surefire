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
package org.apache.maven.plugin.surefire.booterclient;

import java.io.IOException;

/**
 * For testing purposes.
 */
public class MainClass {
    public static void main(String... args) throws IOException {
        if ("fail".equals(args[0])) {
            System.exit(1);
        } else {
            System.out.println(":maven-surefire-event:\u0003:bye:");
            String byeAck = ":maven-surefire-command:\u0007:bye-ack:";
            byte[] cmd = new byte[byeAck.length()];
            int len = System.in.read(cmd);
            if (len != -1 && new String(cmd, 0, len).equals(byeAck)) {
                System.exit(0);
            }
            System.exit(1);
        }
    }
}
