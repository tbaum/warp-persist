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
package com.wideplay.warp.persist.db4o;

import com.db4o.DatabaseReadOnlyException;
import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.config.Configuration;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.wideplay.warp.persist.PersistenceService;
import com.wideplay.warp.persist.Transactional;
import com.wideplay.warp.persist.UnitOfWork;
import org.testng.annotations.Test;

/**
 * @author Robbie Vanbrabant
 */
@Test(suiteName = "db4o")
public class Db4oHostKindTest {
    @Test(expectedExceptions = DatabaseReadOnlyException.class)
    public void FileHostKindDetectsConfigurationObject() {
        Injector injector = Guice.createInjector(PersistenceService.usingDb4o()
                .across(UnitOfWork.TRANSACTION)
                .buildModule(),

                new AbstractModule() {
                    protected void configure() {
                        bindConstant().annotatedWith(Db4Objects.class).to("TestDatabase.data");
                        Configuration config = Db4o.newConfiguration();
                        config.readOnly(true); // so we can test it gets picked up
                        bind(Configuration.class).toInstance(config);
                    }
                });

        injector.getInstance(ReadOnlyDb4oDao.class).persist(new Db4oTestObject("myText"));
    }

    @Test
    public void FileHostKindDoesNotRequireConfigurationObject() {
        Injector injector = Guice.createInjector(PersistenceService.usingDb4o()
                .across(UnitOfWork.TRANSACTION)
                .buildModule(),

                new AbstractModule() {
                    protected void configure() {
                        bindConstant().annotatedWith(Db4Objects.class).to("TestDatabase.data");
                    }
                });

        injector.getInstance(ReadOnlyDb4oDao.class).persist(new Db4oTestObject("myText"));
        injector.getInstance(ObjectServer.class).close();

    }


    public static class ReadOnlyDb4oDao {
        static ObjectContainer oc;

        @Inject
        public ReadOnlyDb4oDao(ObjectContainer oc) {
            ReadOnlyDb4oDao.oc = oc;
        }

        @Transactional
        public <T> void persist(T t) {
            assert !oc.ext().isClosed() : "oc is not open";
            oc.set(t);
        }

        @Transactional
        public <T> boolean contains(T t) {
            return oc.ext().isStored(t);
        }
    }
}
