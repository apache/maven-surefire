package jira1727;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FailureInTestProvider.class)
class FailureInTestTemplateProviderTest
{
    @TestTemplate
    void templatedTest() {
    }
}
