/**
 * Copyright (C) 2009 Wideplay Interactive.
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
package com.wideplay.warp.persist.hibernate;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;
import com.wideplay.codemonkey.web.startup.Initializer;
import com.wideplay.warp.persist.PersistenceService;
import static com.wideplay.warp.persist.TransactionType.READ_ONLY;
import com.wideplay.warp.persist.Transactional;
import com.wideplay.warp.persist.UnitOfWork;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Robbie Vanbrabant
 */
public class ReadOnlyTransactionsTest {
    private Injector injector;
    
    @BeforeClass
    public void pre() {
        injector = Guice.createInjector(PersistenceService.usingHibernate()
            .across(UnitOfWork.TRANSACTION)
            .forAll(Matchers.any())
            .buildModule(),
                new AbstractModule() {
                    protected void configure() {
                        bind(Configuration.class).toInstance(new AnnotationConfiguration()
                            .addAnnotatedClass(ReadOnlyTransactionalObject.class)
                            .setProperties(Initializer.loadProperties("spt-persistence.properties")));
                    }
                });

        //startup persistence
        injector.getInstance(PersistenceService.class).start();
    }


    @AfterClass
    void post() {
        injector.getInstance(PersistenceService.class).shutdown();
    }

    @Test
    public void testReadOnlyTxRestoresSessionFlushMode() {
        final ReadOnlyTransactionalObject txnal = injector.getInstance(ReadOnlyTransactionalObject.class);
        Session session = txnal.runReadOnlyTxnAndReturnSession();

        // because the session gets closed in UnitOfWork.TRANSACTION,
        // we do NOT reset the flushmode in the interceptor
            Assert.assertTrue(session.getFlushMode() == FlushMode.MANUAL,
                    "FlushMode has been reset with UnitOfWork.TRANSACTION and read-only transactions, " +
                            "this means the session was not closed!");
    }

    public static class ReadOnlyTransactionalObject {
        @Inject
        Session session;
        
        @Transactional(type = READ_ONLY)
        public Session runReadOnlyTxnAndReturnSession() {
            Assert.assertTrue(session.getFlushMode() == FlushMode.MANUAL,
                    "FlushMode is not set to MANUAL with a read only transaction.");
            return session;
        }
    }
}
