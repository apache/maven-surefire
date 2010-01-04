/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.surefire.junitcore;

import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.Failure;
import org.junit.runner.Description;
import org.junit.runner.Result;

import java.util.*;

/**
 * Demultiplexes threaded running og tests into something that does not look threaded.
 * Essentially makes a threaded junit core RunListener behave like something like a
 * junit4 reporter can handle.
 * The disadvantage of this demultiplexer is that there's no results being echoed to the
 * console as tests are being run
 * All results come like ketchup out of the bottle.
 *
 * This class is basically copied from org.jdogma.junit.DemultiplexingRunListener to keep
 * required external dependencies minimal.
 * @author Kristian Rosenvold, kristian.rosenvold@gmail com
 */
class DemultiplexingRunListener extends RunListener {
    private final Map<String, RecordingRunListener> classList = new HashMap<String, RecordingRunListener>();
    private final RunListener realtarget;

    public DemultiplexingRunListener(RunListener realtarget) {
        this.realtarget = realtarget;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
        // Do nothing. We discard this event because it's basically meaningless
    }

    @Override
    public void testRunFinished(Result outerResult) throws Exception {
        for (RecordingRunListener classReport : classList.values()) {
            classReport.replay( realtarget);
        }
    }

    @Override
    public void testStarted(Description description) throws Exception {
        getOrCreateClassReport(description).testStarted( description);
    }

    @Override
    public void testFinished(Description description) throws Exception {
        getClassReport( description).testFinished(description);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        getClassReport( failure.getDescription()).testFailure( failure);
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
        getClassReport(failure.getDescription()).testAssumptionFailure( failure);
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        getClassReport(description).testIgnored(description);
    }


    RecordingRunListener getClassReport(Description description) {
        synchronized ( classList){
          return classList.get( description.getClassName());
        }
    }

    private RecordingRunListener getOrCreateClassReport(Description description) throws Exception {
        RecordingRunListener result;
        synchronized (classList) {
            result = classList.get(description.getClassName());
            if (result == null) {
                result = new RecordingRunListener();
                result.testRunStarted( description);
                classList.put(description.getClassName(), result);
            }
        }
        return result;
    }

    public class RecordingRunListener extends RunListener {
        private volatile Description testRunStarted;
        private final List<Description> testStarted = Collections.synchronizedList(new ArrayList<Description>());
        private final List<Description> testFinished =  Collections.synchronizedList(new ArrayList<Description>());
        private final List<Failure> testFailure =  Collections.synchronizedList(new ArrayList<Failure>());
        private final List<Failure> testAssumptionFailure =  Collections.synchronizedList(new ArrayList<Failure>());
        private final List<Description> testIgnored =  Collections.synchronizedList(new ArrayList<Description>());
        private final Result resultForThisClass = new Result();
        private final RunListener classRunListener = resultForThisClass.createListener();



        @Override
        public void testRunStarted(Description description) throws Exception {
            this.testRunStarted = description;
        }

        @Override
        public void testRunFinished(Result result) throws Exception {
            throw new IllegalStateException("This method should not be called on the recorder");
        }

        @Override
        public void testStarted(Description description) throws Exception {
            testStarted.add( description);
            classRunListener.testStarted( description);
        }

        @Override
        public void testFinished(Description description) throws Exception {
            testFinished.add( description);
            classRunListener.testFinished(description);
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            testFailure.add( failure);
            classRunListener.testFailure( failure);
        }

        @Override
        public void testAssumptionFailure(Failure failure) {
            testAssumptionFailure.add( failure);
        }

        @Override
        public void testIgnored(Description description) throws Exception {
            testIgnored.add(  description);
        }

        public void replay(RunListener target) throws Exception {
            target.testRunStarted (testRunStarted);

            for( Description description : testStarted) {
                target.testStarted( description);
            }
            for( Failure failure : testFailure) {
                target.testFailure( failure);
            }
            for( Description description : testIgnored) {
                target.testIgnored( description);
            }
            for( Failure failure : testAssumptionFailure) {
                target.testAssumptionFailure( failure);
            }
            for( Description description : testFinished) {
                target.testFinished( description);
            }
            target.testRunFinished( resultForThisClass);
        }


    }

}
