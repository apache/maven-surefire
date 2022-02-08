package org.apache.failsafe.its.serviceloader.provider.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.failsafe.its.serviceloader.api.model.Student;

import java.util.Arrays;
import java.util.List;

public class InternalStudentService {

    public int retrieveStudentDept(String name) {
        // Mimic retrieving from database
        return name.length();
    }

    public List<Student> retrieveStudents() {
        // Mimic retrieving from database
        Student anne = new Student( "Anne" );
        Student peter = new Student( "Peter" );
        return Arrays.asList( anne, peter );
    }
}
