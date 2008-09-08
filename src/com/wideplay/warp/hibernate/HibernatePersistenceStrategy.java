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
package com.wideplay.warp.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.wideplay.warp.persist.*;
import org.aopalliance.intercept.MethodInterceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Robbie Vanbrabant
 */
public class HibernatePersistenceStrategy implements PersistenceStrategy {
    private final Configuration configuration;
    private final Class<? extends Annotation> annotation;

    private HibernatePersistenceStrategy(HibernatePersistenceStrategyBuilder builder) {
        this.configuration = builder.configuration;
        this.annotation = builder.annotation;
    }

    public PersistenceModule getBindings(final PersistenceConfiguration config) {
        return new HibernatePersistenceModule(config);
    }

    class HibernatePersistenceModule extends AbstractPersistenceModule {
        private final PersistenceConfiguration config;
        private WorkManager workManager;
        private Provider<SessionFactory> sfProvider;
        private Provider<Session> sessionProvider;
        private PersistenceService pService;
        // needed for bindings created in the constructor.
        private final List<Module> scheduledBindings = new ArrayList<Module>();

        private HibernatePersistenceModule(PersistenceConfiguration config) {
            super(annotation);
            this.config = config;
            // Need instance here for the work manager.
            String annotationDebugString = annotation != null ? annotation.getSimpleName() : "";
            this.sfProvider = new SessionFactoryProvider(getConfigurationKey(), annotationDebugString);
            // Need instance here for the interceptors.
            this.sessionProvider = new SessionProvider(sfProvider);
            // Need WorkManager here so we can register it on the SPR filter if the UnitOfWork is REQUEST
            this.workManager = new HibernateWorkManager(sfProvider, config.getUnitOfWork(), annotationDebugString);
            // Needs to be able to initialize Provider<SessionFactory>
            this.pService = new HibernatePersistenceService(sfProvider);
        }
        
        @Override
        protected void configure() {
            for (Module m : scheduledBindings) install(m);
            
            bindSpecial(SessionFactory.class).toProvider(sfProvider);
            bindSpecial(Session.class).toProvider(sessionProvider);
            bindSpecial(WorkManager.class).toInstance(workManager);
            bindSpecial(PersistenceService.class).toInstance(pService);

            MethodInterceptor txInterceptor = new HibernateLocalTxnInterceptor(sessionProvider);
            bindTransactionInterceptor(config, txInterceptor);

            // Set up Dynamic Finders.
            MethodInterceptor finderInterceptor = new HibernateFinderInterceptor(sessionProvider);
            bindFinderInterceptor(finderInterceptor);
            bindDynamicAccessors(config.getAccessors(), finderInterceptor);
        }

        /**
         * Gets the Key to which the Hibernate Configuration has been bound.
         */
        private Key<Configuration> getConfigurationKey() {
            final Key<Configuration> key = key(Configuration.class);
            if (inMultiModulesMode()) {
                if (configuration != null) {
                    scheduledBindings.add(new AbstractModule() {
                        protected void configure() {
                            bind(key).toInstance(configuration);
                        }
                    });
                }
            }
            return key;
        }

        public WorkManager publishWorkManager() {
            return unitOfWorkRequest(config) ? this.workManager : null;
        }

        public PersistenceService publishPersistenceService() {
            return unitOfWorkRequest(config) ? this.pService : null;
        }
    }

    public static HibernatePersistenceStrategyBuilder builder() {
        return new HibernatePersistenceStrategyBuilder();
    }

    public static class HibernatePersistenceStrategyBuilder implements PersistenceStrategyBuilder<HibernatePersistenceStrategy> {
        private Configuration configuration;
        private Class<? extends Annotation> annotation;

        public HibernatePersistenceStrategyBuilder configuration(Configuration config) {
            this.configuration = config;
            return this;
        }
        public HibernatePersistenceStrategyBuilder annotatedWith(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
            return this;
        }
        public HibernatePersistenceStrategy build() {
            return new HibernatePersistenceStrategy(this);
        }
    }
}
