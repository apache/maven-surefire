package jira1727;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ErrorInTestProvider.class)
class ErrorInTestTemplateProviderTest
{
    @TestTemplate
    void templatedTest() {
    }
}
