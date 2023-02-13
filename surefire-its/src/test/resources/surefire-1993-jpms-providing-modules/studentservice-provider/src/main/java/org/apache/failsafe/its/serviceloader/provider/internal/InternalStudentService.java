package org.apache.failsafe.its.serviceloader.provider.internal;

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
