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

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.wideplay.warp.persist.Defaults;
import com.wideplay.warp.persist.PersistenceStrategy;
import com.wideplay.warp.persist.Transactional;
import com.wideplay.warp.persist.UnitOfWork;
import com.wideplay.warp.persist.dao.Finder;
import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Base module for persistence strategies that holds a bunch
 * of utility methods for easier configuration.
 * @author Robbie Vanbrabant
 */
public abstract class AbstractPersistenceModule extends AbstractModule implements PersistenceModule {
    private final PersistenceConfiguration config;
    private final Class<? extends Annotation> annotation;
    private final List<ClassAndMethodMatcher> transactionMatchers; 

    /**
     * @param configuration the non-{@code null} PersistenceConfiguration obtained from
     *               {@link PersistenceStrategy#getBindings(PersistenceConfiguration)}.
     * @param unitAnnotation the unit annotation or {@code null} if there is none
     */
    protected AbstractPersistenceModule(final PersistenceConfiguration configuration, Class<? extends Annotation> unitAnnotation) {
        this.annotation = unitAnnotation;

        // Do NOT use configuration.getTransactionMatchers() directly after we set the local variable.
        this.transactionMatchers =
                MultiModules.getTransactionMatchersInEffect(configuration.getTransactionMatchers(), this.annotation);

        // protect from programming mistakes
        this.config = new PersistenceConfiguration() {
            public UnitOfWork getUnitOfWork() {
                return configuration.getUnitOfWork();
            }
            public List<ClassAndMethodMatcher> getTransactionMatchers() {
                throw new UnsupportedOperationException();
            }
            public Set<Class<?>> getDynamicAccessors() {
                return configuration.getDynamicAccessors();
            }
        };
    }

    /**
     * To be implemented by subclasses.
     * @see com.google.inject.AbstractModule#configure()
     */
    protected abstract void configure();

    /**
     * Bind with an optional unit annotation type, which is a binding annotation used only
     * in multimodules mode and specified in the constructor of this class.
     * @param tClass the type to bind
     * @return the next step in the binding builder
     */
    protected <T> com.google.inject.binder.LinkedBindingBuilder<T> bindWithUnitAnnotation(java.lang.Class<T> tClass) {
        if (inMultiModulesMode()) {
            return super.bind(tClass).annotatedWith(annotation);
        } else {
            return super.bind(tClass);
        }
    }

    /**
     * Binds the finder and transaction interceptor for use with Dynamic Accessors. The transaction interceptor
     * will use the same matchers used for regular transactions.
     * 
     * @param finderInterceptor the finder interceptor to bind for dynamic accessors
     * @param txInterceptor the transaction interceptor to use for transactional dynamic accessors
     */
    protected void bindTransactionalDynamicAccessors(final MethodInterceptor finderInterceptor,
                                                     final MethodInterceptor txInterceptor) {
        MethodInterceptor transactionalFinderInterceptor =
                DynamicFinders.newTransactionalDynamicFinderInterceptor(finderInterceptor, txInterceptor, transactionMatchers);
        
        bindDynamicAccessors(finderInterceptor, transactionalFinderInterceptor);
    }

    /**
     * Bind the transaction interceptor.
     * @param txInterceptor the transaction interceptor to bind
     */
    protected void bindTransactionInterceptor(MethodInterceptor txInterceptor) {
        // We support forAll, and assume the user knows what he/she is doing.
        for (ClassAndMethodMatcher matcher : transactionMatchers) {
            bindInterceptor(matcher.getClassMatcher(), matcher.getMethodMatcher(), txInterceptor);
        }
    }

    /**
     * Binds a finder interceptor with support for multiple modules. When the user specifies
     * an annotation to bind the module to, we match on {@code @Finder(unit=UserAnnotation.class)}.
     * @param finderInterceptor the finder interceptor to bind
     */
    protected void bindFinderInterceptor(MethodInterceptor finderInterceptor) {
        ClassAndMethodMatcher matcher = MultiModules.getFinderMatcherInEffect(this.annotation);
        bindInterceptor(matcher.getClassMatcher(), matcher.getMethodMatcher(), finderInterceptor);
    }

    /**
     * @return whether we're in multimodules mode or not
     */
    protected boolean inMultiModulesMode() {
        return this.annotation != null;
    }

    /**
     * @return whether the {@link com.wideplay.warp.persist.UnitOfWork#REQUEST} was configured or not
     */
    protected boolean unitOfWorkRequest() {
        return UnitOfWork.REQUEST == config.getUnitOfWork();
    }

    /**
     * Generates a key for the given class, with an optional unit annotation (multimodules mode).
     * @param clazz the type to bind
     * @param <T> the bound type
     * @return the generated key
     */
    protected <T> Key<T> keyWithUnitAnnotation(Class<T> clazz) {
        if (inMultiModulesMode()) {
            return Key.get(clazz, annotation);
        }
        return Key.get(clazz);
    }

    private void bindDynamicAccessors(MethodInterceptor finderInterceptor,
                                      MethodInterceptor transactionalFinderInterceptor) {

        for (Class accessor : config.getDynamicAccessors()) {
            MethodInterceptor interceptor =
                    DynamicFinders.getDynamicFinderInterceptor(accessor, finderInterceptor,
                            transactionalFinderInterceptor, transactionMatchers);

            if (accessor.isInterface()) {
                bindDynamicAccessorInterface(accessor, interceptor);
            } else {
                bindDynamicAccessorClass(accessor, interceptor);
            }
        }
    }

    @SuppressWarnings("unchecked") // Proxies are not generic.
    private void bindDynamicAccessorClass(Class accessor, MethodInterceptor finderInterceptor) {
        for (Method method : accessor.getMethods()) {
            validateFinder(method.getAnnotation(Finder.class), method);
            validateTransactional(method);
        }
        
        Object dynamicFinderInstance = DynamicFinders.newDynamicFinder(accessor, finderInterceptor);
        bindWithUnitAnnotation(accessor).toInstance(dynamicFinderInstance);
    }
    
    @SuppressWarnings("unchecked") // Proxies are not generic.
    private void bindDynamicAccessorInterface(Class accessor, MethodInterceptor finderInterceptor) {
        for (Method method : accessor.getMethods()) {
            Finder finder = method.getAnnotation(Finder.class);
            if (finder == null) {
                addError(method + " has been specified as a Dynamic Accessor but does not have the @Finder annotation.");
            } else {
                validateFinder(finder, method);
            }
            validateTransactional(method);
        }

        Object dynamicAccessorInstance = DynamicFinders.newDynamicAccessor(accessor, finderInterceptor);
        bindWithUnitAnnotation(accessor).toInstance(dynamicAccessorInstance);
    }

    private void validateFinder(Finder finder, Method method) {
        if (finder != null && finder.unit() == Defaults.DefaultUnit.class && inMultiModulesMode()) {
             addError(String.format("%s is a Dynamic Finder but does not have the unit annotation '%s'. " +
                     "Specify as @Finder(unit=%s.class)", method, this.annotation, this.annotation.getSimpleName()));
        }
    }

    private void validateTransactional(Method method) {
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional != null && transactional.unit() == Defaults.DefaultUnit.class &&
                inMultiModulesMode()) {
             addError(String.format("%s is @Transactional but does not have the unit annotation '%s'. " +
                     "Specify as @Transactional(unit=%s.class)", method, this.annotation, this.annotation.getSimpleName()));
        }
    }
}
