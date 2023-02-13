package org.apache.failsafe.its.serviceloader.provider.service;

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
