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
package com.wideplay.warp.persist.spi;

import com.google.inject.matcher.Matcher;
import com.wideplay.warp.persist.Defaults;
import com.wideplay.warp.persist.PersistenceMatchers;
import com.wideplay.warp.persist.internal.InternalPersistenceMatchers;
import com.wideplay.warp.persist.internal.TransactionMatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Utilities related to Warp Persist's support for multiple persistence modules per injector.
 * 
 * @author Robbie Vanbrabant
 */
public class MultiModules {
    private MultiModules() {}

    /**
     * Applies Warp Persist defaults and the given unit annotation to the user-configured list of transaction matchers.
     *
     * @param txMatchers non-{@code null} list of transaction matchers, empty means none were configured
     * @param unitAnnotation the unit annotation or {@code null} if there is none
     * @return the transaction matchers that will be used, or the given list if none apply
     */
    public static List<ClassAndMethodMatcher> getTransactionMatchersInEffect(List<ClassAndMethodMatcher> txMatchers,
                                                                          Class<? extends Annotation> unitAnnotation) {
        if (txMatchers.size() == 0) {
            if (unitAnnotation != null) {
                return Collections.<ClassAndMethodMatcher>singletonList(new TransactionMatcher(Defaults.TRANSACTION_MATCHER.getClassMatcher(),
                                                        PersistenceMatchers.transactionalWithUnit(unitAnnotation)));
            } else {
                return Collections.<ClassAndMethodMatcher>singletonList(new TransactionMatcher());
            }

        }
        return txMatchers;
    }

    /**
     * Gets the finder matcher effect after applying an optional unit annotation.
     *
     * @param unitAnnotation a unit annotation or {@code null}
     * @return the finder matcher
     */
    public static ClassAndMethodMatcher getFinderMatcherInEffect(final Class<? extends Annotation> unitAnnotation) {
        return new ClassAndMethodMatcher() {
            public Matcher<? super Class<?>> getClassMatcher() {
                return Defaults.FINDER_MATCHER.getClassMatcher();
            }

            public Matcher<? super Method> getMethodMatcher() {
                if (unitAnnotation == null) return Defaults.FINDER_MATCHER.getMethodMatcher();
                else return InternalPersistenceMatchers.finderWithUnit(unitAnnotation);
            }
        };
    }
}
