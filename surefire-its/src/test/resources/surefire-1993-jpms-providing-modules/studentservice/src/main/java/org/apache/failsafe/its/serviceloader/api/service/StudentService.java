package org.apache.failsafe.its.serviceloader.api.service;

import org.apache.failsafe.its.serviceloader.api.model.Student;

import java.util.List;

public interface StudentService {
    public List<Student> retrieveStudents();

    public String retrieveStudentName( int number );

    public int retrieveTotalStudentDept();
}
