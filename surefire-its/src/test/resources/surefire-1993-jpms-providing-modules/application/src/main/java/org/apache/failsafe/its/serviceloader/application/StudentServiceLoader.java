package org.apache.failsafe.its.serviceloader.application;

import org.apache.failsafe.its.serviceloader.api.service.StudentService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;


public class StudentServiceLoader {

    public StudentService getStudentService(){
        Optional<StudentService> studentService = ServiceLoader.load( StudentService.class ).findFirst();
        return studentService.orElseThrow(() -> {
            throw new RuntimeException( "Could not find StudentService implementation" );
        } );
    }

    public List<StudentService> getStudentServices(){
        ServiceLoader<StudentService> loader = ServiceLoader.load( StudentService.class );
        List<StudentService> studentServices = new ArrayList<>();
        for ( StudentService studentService : loader ) {
            studentServices.add(studentService);
        }
        return studentServices;
    }
}
