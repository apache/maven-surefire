package org.apache.maven.surefire.booter;

/**
 * Constants used by the serializer/deserializer
 * @author Kristian Rosenvold
 */
public interface BooterConstants
{
    String INCLUDES_PROPERTY_PREFIX = "includes";
    String EXCLUDES_PROPERTY_PREFIX = "excludes";
    String DIRSCANNER_PROPERTY_PREFIX = "dirscanner.";
    String DIRSCANNER_OPTIONS = "directoryScannerOptions";
    String REPORT_PROPERTY_PREFIX = "report.";
    String PARAMS_SUFIX = ".params";
    String TYPES_SUFIX = ".types";
    String CLASSPATH_URL = "classPathUrl.";
    String SUREFIRE_CLASSPATHURL = "surefireClassPathUrl.";
    String CHILD_DELEGATION = "childDelegation";
    String ENABLE_ASSERTIONS = "enableAssertions";
    String USESYSTEMCLASSLOADER = "useSystemClassLoader";
    String USEMANIFESTONLYJAR = "useManifestOnlyJar";
    String FAILIFNOTESTS = "failIfNoTests";
    String ISTRIMSTACKTRACE = "isTrimStackTrace";
    String REPORTSDIRECTORY = "reportsDirectory";
    String TESTNGVERSION = "testNgVersion";
    String TESTNG_CLASSIFIER = "testNgClassifier";
    String REQUESTEDTEST = "requestedTest";
    String SOURCE_DIRECTORY = "testSuiteDefinitionTestSourceDirectory";
    String TEST_CLASSES_DIRECTORY = "testClassesDirectory";
    String TEST_SUITE_XML_FILES = "testSuiteXmlFiles";
    String PROVIDER_CONFIGURATION = "providerConfiguration";
    String FORKTESTSET = "forkTestSet";
}
