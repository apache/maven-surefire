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

import org.apache.maven.surefire.junitplatform.inaccessible.VisibleSub;

/**
 * Same shape as Guava's {@code IterablesTest} in #3511. {@link #use()} first mentions an enum declared inside a
 * class this package cannot access, so javac lists that class in our {@code InnerClasses} before the member
 * classes below. Java 8 and 11 then throw an {@link IllegalAccessError} from {@code getEnclosingClass()} on them.
 */
class OuterWithInaccessibleReference {
    static Object[] use() {
        return new Object[] {VisibleSub.Kind.ONE, new First(), new Second(), new Third()};
    }

    static class First {}

    static class Second {}

    static class Third {}
}
