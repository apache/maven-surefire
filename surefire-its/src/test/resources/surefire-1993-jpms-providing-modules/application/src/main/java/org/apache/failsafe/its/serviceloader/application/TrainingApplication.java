package org.apache.failsafe.its.serviceloader.application;

import org.apache.failsafe.its.serviceloader.api.model.Student;
import org.apache.failsafe.its.serviceloader.api.service.StudentService;

import java.util.Arrays;
import java.util.List;

public class TrainingApplication {

    private StudentService studentService;
    private List<String> modules;

    public TrainingApplication() {
        String modulePath = System.getProperty( "jdk.module.path", "" );
        System.out.printf( "Java Module Path:%n%s%n%n", modulePath.replace( ":", System.lineSeparator() ) );
        this.modules = Arrays.asList( modulePath.split( ":" ) );
        this.studentService = new StudentServiceLoader().getStudentService();
    }

    public List<String> getModules() {
        return this.modules;
    }

}

