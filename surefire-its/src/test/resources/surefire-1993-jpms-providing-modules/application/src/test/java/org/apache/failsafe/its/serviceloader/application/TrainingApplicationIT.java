package org.apache.failsafe.its.serviceloader.application;

import org.apache.failsafe.its.serviceloader.api.model.Student;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainingApplicationIT {

    private TrainingApplication trainingApplication = new TrainingApplication();

    @Test
    public void testModules() {
        List<String> modules = trainingApplication.getModules();
        assertThat( modules ).isNotEmpty();
    }
}
