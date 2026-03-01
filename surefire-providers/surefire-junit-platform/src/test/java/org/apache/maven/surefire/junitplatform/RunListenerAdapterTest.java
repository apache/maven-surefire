/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.junitplatform;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.Stoppable;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.opentest4j.TestSkippedException;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * Unit tests for {@link RunListenerAdapter}.
 *
 * @since 2.22.0
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RunListenerAdapterTest {
    private static final ConfigurationParameters CONFIG_PARAMS = mock(ConfigurationParameters.class);

    private static final OutputDirectoryProvider OUTPUT_DIRECTORY = mock(OutputDirectoryProvider.class);

    private TestReportListener<TestOutputReportEntry> listener;

    private RunListenerAdapter adapter;

    @Before
    public void setUp() {
        listener = mock(TestReportListener.class);
        adapter = new RunListenerAdapter(listener, Stoppable.NOOP);
        adapter.testPlanExecutionStarted(TestPlan.from(false, emptyList(), CONFIG_PARAMS, OUTPUT_DIRECTORY));
        adapter.setRunMode(NORMAL_RUN);
    }

    @Test
    public void notifiedWithCorrectNamesWhenMethodExecutionStarted() throws Exception {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);

        TestPlan testPlan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Luke's Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(testPlan);

        TestIdentifier methodIdentifier =
                identifiersAsParentOnTestPlan(testPlan, newClassDescriptor(), newMethodDescriptor());

        adapter.executionStarted(methodIdentifier);
        verify(listener).testStarting(entryCaptor.capture());

        ReportEntry entry = entryCaptor.getValue();
        assertEquals(MY_TEST_METHOD_NAME, entry.getName());
        assertEquals(MyTestClass.class.getName(), entry.getSourceName());
        assertNull(entry.getStackTraceWriter());
    }

    @Test
    public void notifiedWithCompatibleNameForMethodWithArguments() throws Exception {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);

        TestPlan testPlan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Luke's Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(testPlan);

        TestIdentifier methodIdentifier =
                identifiersAsParentOnTestPlan(testPlan, newClassDescriptor(), newMethodDescriptor(String.class));

        adapter.executionStarted(methodIdentifier);
        verify(listener).testStarting(entryCaptor.capture());

        ReportEntry entry = entryCaptor.getValue();
        assertEquals(MY_TEST_METHOD_NAME + "(String)", entry.getName());
        assertNull(entry.getNameText());
        assertEquals(MyTestClass.class.getName(), entry.getSourceName());
        assertNull(entry.getSourceText());
        assertNull(entry.getStackTraceWriter());
    }

    @Test
    public void notifiedEagerlyForTestSetWhenClassExecutionStarted() throws Exception {
        EngineDescriptor engine = newEngineDescriptor();
        TestDescriptor parent = newClassDescriptor();
        engine.addChild(parent);
        TestDescriptor child = newMethodDescriptor();
        parent.addChild(child);
        TestPlan plan = TestPlan.from(false, singletonList(engine), CONFIG_PARAMS, OUTPUT_DIRECTORY);

        String className = MyTestClass.class.getName();

        adapter.testPlanExecutionStarted(plan);
        adapter.executionStarted(TestIdentifier.from(engine));
        adapter.executionStarted(TestIdentifier.from(parent));
        verify(listener)
                .testSetStarting(new SimpleReportEntry(NORMAL_RUN, 0x0000000100000000L, className, null, null, null));
        verifyNoMoreInteractions(listener);

        adapter.executionStarted(TestIdentifier.from(child));
        verify(listener)
                .testStarting(new SimpleReportEntry(
                        NORMAL_RUN, 0x0000000100000001L, className, null, MY_TEST_METHOD_NAME, null));
        verifyNoMoreInteractions(listener);

        adapter.executionFinished(TestIdentifier.from(child), successful());
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        verify(listener).testSucceeded(report.capture());
        assertThat(report.getValue().getRunMode()).isEqualTo(NORMAL_RUN);
        assertThat(report.getValue().getTestRunId()).isEqualTo(0x0000000100000001L);
        assertThat(report.getValue().getSourceName()).isEqualTo(className);
        assertThat(report.getValue().getSourceText()).isNull();
        assertThat(report.getValue().getName()).isEqualTo(MY_TEST_METHOD_NAME);
        assertThat(report.getValue().getNameText()).isNull();
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getSystemProperties()).isEmpty();
        verifyNoMoreInteractions(listener);

        adapter.executionFinished(TestIdentifier.from(parent), successful());
        report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        verify(listener).testSetCompleted(report.capture());
        assertThat(report.getValue().getSourceName()).isEqualTo(className);
        assertThat(report.getValue().getName()).isNull();
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getSystemProperties()).isNotEmpty();
        verifyNoMoreInteractions(listener);

        adapter.executionFinished(TestIdentifier.from(engine), successful());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void displayNamesInClassAndMethods() throws Exception {
        EngineDescriptor engine = newEngineDescriptor();
        TestDescriptor parent = newClassDescriptor("parent");
        engine.addChild(parent);

        UniqueId id1 = parent.getUniqueId().append(MyTestClass.class.getName(), MY_NAMED_TEST_METHOD_NAME);
        Method m1 = MyTestClass.class.getDeclaredMethod(MY_NAMED_TEST_METHOD_NAME);
        TestDescriptor child1 = new TestMethodTestDescriptorWithDisplayName(id1, MyTestClass.class, m1, "dn1");
        parent.addChild(child1);

        UniqueId id2 = parent.getUniqueId().append(MyTestClass.class.getName(), MY_TEST_METHOD_NAME);
        Method m2 = MyTestClass.class.getDeclaredMethod(MY_TEST_METHOD_NAME, String.class);
        TestDescriptor child2 = new TestMethodTestDescriptor(
                id2,
                MyTestClass.class,
                m2,
                Collections::emptyList,
                new DefaultJupiterConfiguration(CONFIG_PARAMS, OUTPUT_DIRECTORY));
        parent.addChild(child2);

        TestPlan plan = TestPlan.from(false, singletonList(engine), CONFIG_PARAMS, OUTPUT_DIRECTORY);

        InOrder inOrder = inOrder(listener);

        adapter.testPlanExecutionStarted(plan);

        adapter.executionStarted(TestIdentifier.from(engine));
        adapter.executionStarted(TestIdentifier.from(parent));
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        inOrder.verify(listener).testSetStarting(report.capture());
        assertThat(report.getValue().getTestRunId()).isEqualTo(0x0000000100000000L);
        assertThat(report.getValue().getSourceName()).isEqualTo(MyTestClass.class.getName());
        assertThat(report.getValue().getSourceText()).isEqualTo("parent");
        assertThat(report.getValue().getName()).isNull();
        assertThat(report.getValue().getSystemProperties()).isEmpty();
        verifyZeroInteractions(listener);

        adapter.executionStarted(TestIdentifier.from(child1));
        inOrder.verify(listener)
                .testStarting(new SimpleReportEntry(
                        NORMAL_RUN,
                        0x0000000100000001L,
                        MyTestClass.class.getName(),
                        "parent",
                        MY_NAMED_TEST_METHOD_NAME,
                        "dn1"));
        inOrder.verifyNoMoreInteractions();

        adapter.executionFinished(TestIdentifier.from(child1), successful());
        report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        inOrder.verify(listener).testSucceeded(report.capture());
        assertThat(report.getValue().getRunMode()).isEqualTo(NORMAL_RUN);
        assertThat(report.getValue().getTestRunId()).isEqualTo(0x0000000100000001L);
        assertThat(report.getValue().getSourceName()).isEqualTo(MyTestClass.class.getName());
        assertThat(report.getValue().getSourceText()).isEqualTo("parent");
        assertThat(report.getValue().getName()).isEqualTo(MY_NAMED_TEST_METHOD_NAME);
        assertThat(report.getValue().getNameText()).isEqualTo("dn1");
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getSystemProperties()).isEmpty();
        inOrder.verifyNoMoreInteractions();

        adapter.executionStarted(TestIdentifier.from(child2));
        inOrder.verify(listener)
                .testStarting(new SimpleReportEntry(
                        NORMAL_RUN,
                        0x0000000100000002L,
                        MyTestClass.class.getName(),
                        "parent",
                        MY_TEST_METHOD_NAME + "(String)",
                        null));
        inOrder.verifyNoMoreInteractions();

        Exception assumptionFailure = new Exception();
        adapter.executionFinished(TestIdentifier.from(child2), aborted(assumptionFailure));
        report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        inOrder.verify(listener).testAssumptionFailure(report.capture());
        assertThat(report.getValue().getRunMode()).isEqualTo(NORMAL_RUN);
        assertThat(report.getValue().getTestRunId()).isEqualTo(0x0000000100000002L);
        assertThat(report.getValue().getSourceName()).isEqualTo(MyTestClass.class.getName());
        assertThat(report.getValue().getSourceText()).isEqualTo("parent");
        assertThat(report.getValue().getName()).isEqualTo(MY_TEST_METHOD_NAME + "(String)");
        assertThat(report.getValue().getNameText()).isNull();
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getSystemProperties()).isEmpty();
        assertThat(report.getValue().getStackTraceWriter()).isNotNull();
        assertThat(report.getValue().getStackTraceWriter().getThrowable().getTarget())
                .isSameAs(assumptionFailure);
        inOrder.verifyNoMoreInteractions();

        adapter.executionFinished(TestIdentifier.from(parent), successful());
        inOrder.verify(listener).testSetCompleted(report.capture());
        assertThat(report.getValue().getSourceName()).isEqualTo(MyTestClass.class.getName());
        assertThat(report.getValue().getSourceText()).isEqualTo("parent");
        assertThat(report.getValue().getName()).isNull();
        assertThat(report.getValue().getNameText()).isNull();
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getSystemProperties()).isNotEmpty();
        assertThat(report.getValue().getStackTraceWriter()).isNull();
        inOrder.verifyNoMoreInteractions();

        adapter.executionFinished(TestIdentifier.from(engine), successful());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void notifiedForUnclassifiedTestIdentifier() {
        EngineDescriptor engine = new EngineDescriptor(UniqueId.forEngine("engine"), "engine") {
            @Override
            public Type getType() {
                return TEST;
            }
        };
        TestPlan plan = TestPlan.from(false, singletonList(engine), CONFIG_PARAMS, OUTPUT_DIRECTORY);

        adapter.testPlanExecutionStarted(plan);
        assertThat((TestPlan) getInternalState(adapter, "testPlan")).isSameAs(plan);
        assertThat((Map) getInternalState(adapter, "testStartTime")).isEmpty();

        adapter.executionStarted(TestIdentifier.from(engine));
        verify(listener)
                .testStarting(new SimpleReportEntry(NORMAL_RUN, 0x0000000100000001L, "engine", null, "engine", null));
        verifyNoMoreInteractions(listener);

        adapter.executionFinished(TestIdentifier.from(engine), successful());
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        verify(listener).testSucceeded(report.capture());
        assertThat(report.getValue().getRunMode()).isEqualTo(NORMAL_RUN);
        assertThat(report.getValue().getTestRunId()).isEqualTo(0x0000000100000001L);
        assertThat(report.getValue().getSourceName()).isEqualTo("engine");
        assertThat(report.getValue().getSourceText()).isNull();
        assertThat(report.getValue().getName()).isEqualTo("engine");
        assertThat(report.getValue().getNameText()).isNull();
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getStackTraceWriter()).isNull();
        assertThat(report.getValue().getSystemProperties()).isEmpty();

        adapter.testPlanExecutionFinished(plan);
        assertThat((TestPlan) getInternalState(adapter, "testPlan")).isNull();
        assertThat((Map) getInternalState(adapter, "testStartTime")).isEmpty();

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void notNotifiedWhenEngineExecutionStarted() {
        adapter.executionStarted(newEngineIdentifier());
        verify(listener, never()).testStarting(any());
    }

    @Test
    public void notifiedWhenMethodExecutionSkipped() throws Exception {
        adapter.executionSkipped(newMethodIdentifier(), "test");
        verify(listener).testSkipped(any());
    }

    @Test
    public void notifiedWithCorrectNamesWhenClassExecutionSkipped() throws Exception {
        EngineDescriptor engineDescriptor = new EngineDescriptor(newId(), "Luke's Plan");
        TestDescriptor classTestDescriptor = newClassDescriptor();
        TestDescriptor method1 = newMethodDescriptor();
        classTestDescriptor.addChild(method1);
        TestDescriptor method2 = newMethodDescriptor();
        classTestDescriptor.addChild(method2);
        engineDescriptor.addChild(classTestDescriptor);
        TestPlan testPlan = TestPlan.from(false, singletonList(engineDescriptor), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(testPlan);

        TestIdentifier classIdentifier =
                identifiersAsParentOnTestPlan(testPlan, newEngineDescriptor(), newClassDescriptor());

        ArgumentCaptor<TestSetReportEntry> entryCaptor1 = ArgumentCaptor.forClass(TestSetReportEntry.class);
        ArgumentCaptor<ReportEntry> entryCaptor2 = ArgumentCaptor.forClass(ReportEntry.class);
        ArgumentCaptor<ReportEntry> entryCaptor3 = ArgumentCaptor.forClass(ReportEntry.class);
        ArgumentCaptor<TestSetReportEntry> entryCaptor4 = ArgumentCaptor.forClass(TestSetReportEntry.class);

        adapter.executionSkipped(classIdentifier, "test");
        verify(listener).testSetStarting(entryCaptor1.capture());
        verify(listener).testSkipped(entryCaptor2.capture());
        verify(listener).testSkipped(entryCaptor3.capture());
        verify(listener).testSetCompleted(entryCaptor4.capture());

        ReportEntry entry1 = entryCaptor1.getValue();
        assertNull(entry1.getName());
        assertEquals(MyTestClass.class.getTypeName(), entry1.getSourceName());

        ReportEntry entry4 = entryCaptor1.getValue();
        assertNull(entry4.getName());
        assertEquals(MyTestClass.class.getTypeName(), entry4.getSourceName());
    }

    @Test
    public void notifiedWhenMethodExecutionAborted() throws Exception {
        adapter.executionFinished(newMethodIdentifier(), aborted(null));
        verify(listener).testAssumptionFailure(any());
    }

    @Test
    public void notifiedWhenClassExecutionAborted() {
        TestSkippedException t = new TestSkippedException("skipped");
        adapter.executionFinished(newClassIdentifier(), aborted(t));
        String source = MyTestClass.class.getName();
        StackTraceWriter stw = new DefaultStackTraceWriter(source, null, t);
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        verify(listener).testSetCompleted(report.capture());
        assertThat(report.getValue().getSourceName()).isEqualTo(source);
        assertThat(report.getValue().getStackTraceWriter()).isEqualTo(stw);
    }

    @Test
    public void notifiedOfContainerFailureWhenErrored() throws Exception {
        adapter.executionFinished(newContainerIdentifier(), failed(new RuntimeException()));
        verify(listener).testError(any());
    }

    @Test
    public void notifiedOfContainerFailureWhenFailed() throws Exception {
        adapter.executionFinished(newContainerIdentifier(), failed(new AssertionError()));
        verify(listener).testFailed(any());
    }

    @Test
    public void notifiedWhenMethodExecutionFailed() throws Exception {
        adapter.executionFinished(newMethodIdentifier(), failed(new AssertionError()));
        verify(listener).testFailed(any());
    }

    @Test
    public void notifiedWhenMethodExecutionFailedWithError() throws Exception {
        adapter.executionFinished(newMethodIdentifier(), failed(new RuntimeException()));
        verify(listener).testError(any());
    }

    @Test
    public void notifiedWithCorrectNamesWhenClassExecutionFailed() {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        TestPlan testPlan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Luke's Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(testPlan);

        adapter.executionFinished(
                identifiersAsParentOnTestPlan(testPlan, newClassDescriptor()), failed(new AssertionError()));
        verify(listener).testFailed(entryCaptor.capture());

        ReportEntry entry = entryCaptor.getValue();
        assertEquals(MyTestClass.class.getTypeName(), entry.getSourceName());
        assertNull(entry.getName());
        assertNotNull(entry.getStackTraceWriter());
        assertNotNull(entry.getStackTraceWriter().getThrowable());
        assertThat(entry.getStackTraceWriter().getThrowable().getTarget()).isInstanceOf(AssertionError.class);
    }

    @Test
    public void notifiedWithCorrectNamesWhenClassExecutionErrored() {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        TestPlan testPlan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Luke's Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(testPlan);

        adapter.executionFinished(
                identifiersAsParentOnTestPlan(testPlan, newClassDescriptor()), failed(new RuntimeException()));
        verify(listener).testError(entryCaptor.capture());

        ReportEntry entry = entryCaptor.getValue();
        assertEquals(MyTestClass.class.getTypeName(), entry.getSourceName());
        assertNull(entry.getName());
        assertNotNull(entry.getStackTraceWriter());
        assertNotNull(entry.getStackTraceWriter().getThrowable());
        assertThat(entry.getStackTraceWriter().getThrowable().getTarget()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void notifiedWithCorrectNamesWhenContainerFailed() throws Exception {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        TestPlan testPlan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Luke's Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(testPlan);

        adapter.executionFinished(newContainerIdentifier(), failed(new RuntimeException()));
        verify(listener).testError(entryCaptor.capture());

        ReportEntry entry = entryCaptor.getValue();
        assertEquals(MyTestClass.class.getTypeName(), entry.getSourceName());
        assertEquals(MY_TEST_METHOD_NAME, entry.getName());
        assertNotNull(entry.getStackTraceWriter());
        assertNotNull(entry.getStackTraceWriter().getThrowable());
        assertThat(entry.getStackTraceWriter().getThrowable().getTarget()).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void notifiedWhenMethodExecutionSucceeded() throws Exception {
        adapter.executionFinished(newMethodIdentifier(), successful());
        verify(listener).testSucceeded(any());
    }

    @Test
    public void notifiedForTestSetWhenClassExecutionSucceeded() {
        EngineDescriptor engineDescriptor = newEngineDescriptor();
        TestDescriptor classDescriptor = newClassDescriptor();
        engineDescriptor.addChild(classDescriptor);
        adapter.testPlanExecutionStarted(
                TestPlan.from(false, singleton(engineDescriptor), CONFIG_PARAMS, OUTPUT_DIRECTORY));
        adapter.executionStarted(TestIdentifier.from(classDescriptor));

        adapter.executionFinished(TestIdentifier.from(classDescriptor), successful());

        String className = MyTestClass.class.getName();

        verify(listener)
                .testSetStarting(new SimpleReportEntry(NORMAL_RUN, 0x0000000100000000L, className, null, null, null));

        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass(SimpleReportEntry.class);
        verify(listener).testSetCompleted(report.capture());
        assertThat(report.getValue().getRunMode()).isEqualTo(NORMAL_RUN);
        assertThat(report.getValue().getTestRunId()).isEqualTo(0x0000000100000000L);
        assertThat(report.getValue().getSourceName()).isEqualTo(className);
        assertThat(report.getValue().getSourceText()).isNull();
        assertThat(report.getValue().getName()).isNull();
        assertThat(report.getValue().getNameText()).isNull();
        assertThat(report.getValue().getStackTraceWriter()).isNull();
        assertThat(report.getValue().getElapsed()).isNotNull();
        assertThat(report.getValue().getSystemProperties()).isNotEmpty();

        verify(listener, never()).testSucceeded(any());

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void notifiedWithParentDisplayNameWhenTestClassUnknown() {
        // Set up a test plan
        TestPlan plan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Luke's Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(plan);

        // Use the test plan to set up child with parent.
        final String parentDisplay = "I am your father";
        TestIdentifier child = newSourcelessChildIdentifierWithParent(plan, parentDisplay, null);
        adapter.executionStarted(child);

        // Check that the adapter has informed Surefire that the test has been invoked,
        // with the parent name as source (since the test case itself had no source).
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        verify(listener).testStarting(entryCaptor.capture());
        assertEquals(parentDisplay, entryCaptor.getValue().getSourceName());
        assertNull(entryCaptor.getValue().getSourceText());
        assertNull(entryCaptor.getValue().getName());
        assertNull(entryCaptor.getValue().getNameText());
    }

    @Test
    public void stackTraceWriterPresentWhenParentHasSource() {
        TestPlan plan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Some Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(plan);

        TestIdentifier child =
                newSourcelessChildIdentifierWithParent(plan, "Parent", ClassSource.from(MyTestClass.class));
        adapter.executionFinished(child, failed(new RuntimeException()));
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        verify(listener).testError(entryCaptor.capture());
        assertNotNull(entryCaptor.getValue().getStackTraceWriter());
    }

    @Test
    public void stackTraceWriterDefaultsToTestClass() {
        TestPlan plan = TestPlan.from(
                false, singletonList(new EngineDescriptor(newId(), "Some Plan")), CONFIG_PARAMS, OUTPUT_DIRECTORY);
        adapter.testPlanExecutionStarted(plan);

        TestIdentifier child = newSourcelessChildIdentifierWithParent(plan, "Parent", null);
        adapter.executionFinished(child, failed(new RuntimeException("message")));
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        verify(listener).testError(entryCaptor.capture());
        assertNotNull(entryCaptor.getValue().getStackTraceWriter());
        assertNotNull(entryCaptor.getValue().getStackTraceWriter().smartTrimmedStackTrace());
        assertNotNull(entryCaptor.getValue().getStackTraceWriter().writeTraceToString());
        assertNotNull(entryCaptor.getValue().getStackTraceWriter().writeTrimmedTraceToString());
    }

    @Test
    public void stackTraceWriterPresentEvenWithoutException() throws Exception {
        adapter.executionFinished(newMethodIdentifier(), failed(null));
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);
        verify(listener).testError(entryCaptor.capture());
        assertNotNull(entryCaptor.getValue().getStackTraceWriter());
    }

    @Test
    public void displayNamesIgnoredInReport() throws NoSuchMethodException {
        TestMethodTestDescriptorWithDisplayName descriptor = new TestMethodTestDescriptorWithDisplayName(
                newId(),
                MyTestClass.class,
                MyTestClass.class.getDeclaredMethod("myNamedTestMethod"),
                "some display name");

        TestIdentifier factoryIdentifier = TestIdentifier.from(descriptor);
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass(ReportEntry.class);

        adapter.executionSkipped(factoryIdentifier, "");
        verify(listener).testSkipped(entryCaptor.capture());

        ReportEntry value = entryCaptor.getValue();

        assertEquals(MyTestClass.class.getName(), value.getSourceName());
        assertNull(value.getSourceText());
        assertEquals("myNamedTestMethod", value.getName());
        assertEquals("some display name", value.getNameText());
    }

    private static TestIdentifier newMethodIdentifier() throws Exception {
        return TestIdentifier.from(newMethodDescriptor());
    }

    private static TestDescriptor newMethodDescriptor(Class<?>... parameterTypes) throws Exception {
        return new TestMethodTestDescriptor(
                UniqueId.forEngine("method"),
                MyTestClass.class,
                MyTestClass.class.getDeclaredMethod(MY_TEST_METHOD_NAME, parameterTypes),
                Collections::emptyList,
                new DefaultJupiterConfiguration(CONFIG_PARAMS, OUTPUT_DIRECTORY));
    }

    private static TestIdentifier newClassIdentifier() {
        return TestIdentifier.from(newClassDescriptor());
    }

    private static TestDescriptor newClassDescriptor(String displayName) {
        JupiterConfiguration jupiterConfiguration = mock(JupiterConfiguration.class);
        DisplayNameGenerator displayNameGenerator = mock(DisplayNameGenerator.class);
        when(displayNameGenerator.generateDisplayNameForClass(MyTestClass.class))
                .thenReturn(displayName);
        when(jupiterConfiguration.getDefaultDisplayNameGenerator()).thenReturn(displayNameGenerator);
        return new ClassTestDescriptor(
                UniqueId.root("class", MyTestClass.class.getName()), MyTestClass.class, jupiterConfiguration) {};
    }

    private static TestDescriptor newClassDescriptor() {
        return new ClassTestDescriptor(
                UniqueId.root("class", MyTestClass.class.getName()),
                MyTestClass.class,
                new DefaultJupiterConfiguration(CONFIG_PARAMS, OUTPUT_DIRECTORY));
    }

    private static TestIdentifier newSourcelessChildIdentifierWithParent(
            TestPlan testPlan, String parentDisplay, TestSource parentTestSource) {
        // A parent test identifier with a name.
        TestDescriptor parent = mock(TestDescriptor.class);
        when(parent.getUniqueId()).thenReturn(newId());
        when(parent.getDisplayName()).thenReturn(parentDisplay);
        when(parent.getLegacyReportingName()).thenReturn(parentDisplay);
        when(parent.getSource()).thenReturn(Optional.ofNullable(parentTestSource));
        when(parent.getType()).thenReturn(CONTAINER);
        TestIdentifier parentId = TestIdentifier.from(parent);

        // The (child) test case that is to be executed as part of a test plan.
        TestDescriptor child = mock(TestDescriptor.class);
        when(child.getUniqueId()).thenReturn(newId());
        when(child.getType()).thenReturn(TEST);
        when(child.getLegacyReportingName()).thenReturn("child");

        // Ensure the child source is null yet that there is a parent -- the special case to be tested.
        when(child.getSource()).thenReturn(Optional.empty());
        when(child.getParent()).thenReturn(Optional.of(parent));
        TestIdentifier childId = TestIdentifier.from(child);

        testPlan.addInternal(childId);
        testPlan.addInternal(parentId);

        return childId;
    }

    private static TestIdentifier newContainerIdentifier() throws Exception {
        return TestIdentifier.from(new TestTemplateTestDescriptor(
                UniqueId.forEngine("method"),
                MyTestClass.class,
                MyTestClass.class.getDeclaredMethod(MY_TEST_METHOD_NAME),
                Collections::emptyList,
                new DefaultJupiterConfiguration(CONFIG_PARAMS, OUTPUT_DIRECTORY)));
    }

    private static TestIdentifier newEngineIdentifier() {
        TestDescriptor testDescriptor = newEngineDescriptor();
        return TestIdentifier.from(testDescriptor);
    }

    private static EngineDescriptor newEngineDescriptor() {
        return new EngineDescriptor(UniqueId.forEngine("engine"), "engine");
    }

    private static TestIdentifier identifiersAsParentOnTestPlan(
            TestPlan plan, TestDescriptor parent, TestDescriptor child) {
        child.setParent(parent);

        TestIdentifier parentIdentifier = TestIdentifier.from(parent);
        TestIdentifier childIdentifier = TestIdentifier.from(child);

        plan.addInternal(parentIdentifier);
        plan.addInternal(childIdentifier);

        return childIdentifier;
    }

    private static TestIdentifier identifiersAsParentOnTestPlan(TestPlan plan, TestDescriptor root) {
        TestIdentifier rootIdentifier = TestIdentifier.from(root);
        plan.addInternal(rootIdentifier);
        return rootIdentifier;
    }

    private static UniqueId newId() {
        return UniqueId.forEngine("engine");
    }

    private static final String MY_TEST_METHOD_NAME = "myTestMethod";
    private static final String MY_NAMED_TEST_METHOD_NAME = "myNamedTestMethod";

    private static class MyTestClass {
        @org.junit.jupiter.api.Test
        void myTestMethod() {}

        @org.junit.jupiter.api.Test
        void myTestMethod(String foo) {}

        @DisplayName("name")
        @org.junit.jupiter.api.Test
        void myNamedTestMethod() {}
    }

    static class TestMethodTestDescriptorWithDisplayName extends AbstractTestDescriptor {
        private TestMethodTestDescriptorWithDisplayName(
                UniqueId uniqueId, Class<?> testClass, Method testMethod, String displayName) {
            super(uniqueId, displayName, MethodSource.from(testClass, testMethod));
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }
    }
}
