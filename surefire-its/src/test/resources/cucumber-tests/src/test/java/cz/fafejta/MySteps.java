package cz.fafejta;

import org.junit.jupiter.api.Assertions;

import io.cucumber.java.en.Given;

public class MySteps {

    @Given("{int} plus {int} is {int}")
    public void sumStep(int a, int b, int c) {
        Assertions.assertEquals(c, a + b);
    }
}
