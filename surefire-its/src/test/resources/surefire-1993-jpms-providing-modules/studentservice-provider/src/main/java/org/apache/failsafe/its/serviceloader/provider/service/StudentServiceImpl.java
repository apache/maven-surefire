package org.apache.failsafe.its.serviceloader.provider.service;

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

import org.apache.failsafe.its.serviceloader.provider.internal.InternalStudentService;
import org.apache.failsafe.its.serviceloader.api.model.Student;
import org.apache.failsafe.its.serviceloader.api.service.StudentService;

import java.util.List;

public class StudentServiceImpl implements StudentService {

    private InternalStudentService internalStudentService = new InternalStudentService();

    @Override
    public String retrieveStudentName(int number) {
        return "Susan " + number;
    }

    @Override
    public List<Student> retrieveStudents() {
        return internalStudentService.retrieveStudents();
    }

    @Override
    public int retrieveTotalStudentDept() {
        List<Student> studentList = internalStudentService.retrieveStudents();
        int totalDept = 0;
        for (Student student : studentList) {
            int studentDept = internalStudentService.retrieveStudentDept(student.getName());
            totalDept += studentDept;
        }
        return totalDept;
    }
}
