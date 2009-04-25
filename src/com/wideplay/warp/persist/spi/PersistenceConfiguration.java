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

import com.wideplay.warp.persist.UnitOfWork;

import java.util.List;
import java.util.Set;

/**
 * Configuration values gathered through the fluent interface API.
 * @author Robbie Vanbrabant
 */
public interface PersistenceConfiguration {
    /**
     * @return the unit of work
     */
    UnitOfWork getUnitOfWork();

    /**
     * Returns the transaction matchers configured using one of the
     * {@link com.wideplay.warp.persist.TransactionStrategyBuilder} {@code forAll(...)} methods.
     * <p>
     * When the list is empty, this means the defaults are in effect. These can be found in the
     * {@link com.wideplay.warp.persist.Defaults} class. We always match on any class, and any method with the
     * {@code @Transactional} annotation. When a unit annotations is used (multiple modules mode) the method matcher
     * also needs to match on that unit annotation; the right matcher can be created by using the
     * {@link com.wideplay.warp.persist.PersistenceMatchers} class.
     * 
     * @return all user-configured transaction matchers or an empty list
     */
    List<ClassAndMethodMatcher> getTransactionMatchers();

    /**
     * Returns the configured Dynamic Accessors, which are
     * Dynamic Finders that are interfaces.
     * @return all configured Dynamic Accessors or an empty set
     */
    Set<Class<?>> getDynamicAccessors();
}
