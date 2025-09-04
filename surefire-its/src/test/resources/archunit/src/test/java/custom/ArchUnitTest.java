package custom;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

@AnalyzeClasses(packages = "custom", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchUnitTest {

    @ArchTest
    public static final ArchRule DTO_IN_PACKAGE_DTO= ArchRuleDefinition.classes()
        .that().haveSimpleNameEndingWith("DTO")
        .should().resideInAPackage("..dto..");
}
