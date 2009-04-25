/**
 * Copyright (C) 2008 Wideplay Interactive.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wideplay.warp.persist;

import com.google.inject.matcher.Matcher;
import static com.google.inject.matcher.Matchers.any;
import com.wideplay.warp.persist.internal.InternalPersistenceMatchers;
import com.wideplay.warp.persist.spi.ClassAndMethodMatcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

/**
 * Configuration default values.
 * @author Robbie Vanbrabant
 */
public class Defaults {
    private Defaults() {}

    public static final UnitOfWork UNIT_OF_WORK = UnitOfWork.TRANSACTION;

    public static final ClassAndMethodMatcher TRANSACTION_MATCHER = new ClassAndMethodMatcher() {
        public Matcher<? super Class<?>> getClassMatcher() {
            return any();
        }
        public Matcher<? super Method> getMethodMatcher() {
            return PersistenceMatchers.transactionalWithUnit(DefaultUnit.class);
        }
    };

    public static final ClassAndMethodMatcher FINDER_MATCHER = new ClassAndMethodMatcher() {
        public Matcher<? super Class<?>> getClassMatcher() {
            return any();
        }
        public Matcher<? super Method> getMethodMatcher() {
            return InternalPersistenceMatchers.finderWithUnit(DefaultUnit.class);
        }
    };

    /**
     * Default persistence unit annotation.
     * @author Robbie Vanbrabant
     * @see com.wideplay.warp.persist.dao.Finder
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DefaultUnit {}
}
