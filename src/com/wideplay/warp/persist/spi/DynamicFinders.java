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

import com.wideplay.warp.persist.internal.AopAllianceCglibAdapter;
import com.wideplay.warp.persist.internal.AopAllianceJdkProxyAdapter;
import com.wideplay.warp.persist.internal.WarpPersistNamingPolicy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Proxy;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory methods that help enable Warp Persist's Dynamic Finder features.
 *
 * @author Robbie Vanbrabant
 */
public class DynamicFinders {
    private DynamicFinders() {}

    /**
     * Creates a new dynamic accessor instance.
     * 
     * @param accessor specification for the accessor instance to create
     * @param finderInterceptor the finder interceptor to use
     * @return a Warp Persist-generated instance of the given accessor
     */
    public static Object newDynamicAccessor(Class<?> accessor,
                                            MethodInterceptor finderInterceptor) {
        return Proxy.newProxyInstance(accessor.getClassLoader(),
                        new Class<?>[] { accessor }, new AopAllianceJdkProxyAdapter(
                        finderInterceptor));
    }

    /**
     * Creates a new dynamic finder instance.
     *
     * @param accessor specification for the accessor instance to create
     * @param finderInterceptor the finder interceptor to use
     * @return a Warp Persist-generated instance of the given dynamic finder
     */
    public static Object newDynamicFinder(Class<?> accessor,
                                            MethodInterceptor finderInterceptor) {
        Enhancer enhancer = new Enhancer();
        enhancer.setNamingPolicy(new WarpPersistNamingPolicy());
        enhancer.setCallback(new AopAllianceCglibAdapter(finderInterceptor));
        enhancer.setSuperclass(accessor);
        
        return enhancer.create();
    }

    /**
     * Combines the given finder and transaction interceptors into a single transactional finder interceptor.
     * 
     * @param finderInterceptor the finder interceptor to use
     * @param txInterceptor the transaction interceptor to use
     * @param txMatchers the transaction matchers, used to decide which methods to transact
     * @return the transactional method interceptor
     */
    public static MethodInterceptor newTransactionalDynamicFinderInterceptor(final MethodInterceptor finderInterceptor,
                                                                      final MethodInterceptor txInterceptor,
                                                                      final Iterable<ClassAndMethodMatcher> txMatchers) {
        return new MethodInterceptor() {
            private ConcurrentMap<Method, Boolean> matcherCache = new ConcurrentHashMap<Method, Boolean>();
            public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
                // Don't care about a theoretical extra run through this, so we don't lock
                if (!matcherCache.containsKey(methodInvocation.getMethod())) {
                    boolean matches = false;
                    for (ClassAndMethodMatcher matcher : txMatchers) {
                        matches |= matcher.getMethodMatcher().matches(methodInvocation.getMethod());
                    }
                    matcherCache.putIfAbsent(methodInvocation.getMethod(), matches);
                }

                if (matcherCache.get(methodInvocation.getMethod())) {
                    return txInterceptor.invoke(new MethodInvocation() {
                        public Object[] getArguments() {
                            return methodInvocation.getArguments();
                        }
                        public Method getMethod() {
                            return methodInvocation.getMethod();
                        }
                        public Object proceed() throws Throwable {
                            return finderInterceptor.invoke(methodInvocation);
                        }
                        public Object getThis() {
                            return methodInvocation.getThis();
                        }
                        public AccessibleObject getStaticPart() {
                            return methodInvocation.getStaticPart();
                        }
                    });
                } else {
                    return finderInterceptor.invoke(methodInvocation);
                }
            }
        };
    }

    /**
     * Returns the interceptor to use for a dynamic accessor / finder
     * chosen from the interceptors specified by the caller. We need this method because we apply transaction matchers
     * to determine if we need to have a transaction-enabled interceptor or not.
     *
     * @param accessor specification for the accessor instance
     * @param txMatchers the effective transaction matchers for the current persistence module
     * @param finderInterceptor the finder interceptor to use when no transaction matcher matches the accessor
     * @param transactionalFinderInterceptor the finder interceptor to use when one or more methods on the accessor
     *                                       match a transaction matcher, this interceptor must also implement everything
     *                                       needed to manage the transactions, possibly created using
     *                                       {@link #newTransactionalDynamicFinderInterceptor(org.aopalliance.intercept.MethodInterceptor, org.aopalliance.intercept.MethodInterceptor, Iterable)}
     * @return a Warp Persist-generated instance of the given dynamic finder
     */
    public static MethodInterceptor getDynamicFinderInterceptor(Class<?> accessor,
                                                                    MethodInterceptor finderInterceptor,
                                                                    MethodInterceptor transactionalFinderInterceptor,
                                                                    Iterable<ClassAndMethodMatcher> txMatchers) {
        boolean matches = false;
        for (ClassAndMethodMatcher matcher : txMatchers) {
            matches |= matcher.getClassMatcher().matches(accessor);
        }
        return matches ? transactionalFinderInterceptor : finderInterceptor;
    }
}
