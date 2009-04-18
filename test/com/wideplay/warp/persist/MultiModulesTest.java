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

import com.google.inject.*;
import com.wideplay.codemonkey.web.startup.Initializer;
import com.wideplay.warp.persist.dao.Finder;
import com.wideplay.warp.persist.hibernate.HibernatePersistenceStrategy;
import org.hibernate.Session;
import org.hibernate.cfg.AnnotationConfiguration;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author Robbie Vanbrabant
 */
public class MultiModulesTest {
    private Injector injector;
    private List<PersistenceService> persistenceServices;

    @BeforeMethod
    public void setUp() {
        // Using dynamic accessors because it allows us to check if the interceptor got applied or not
        PersistenceStrategy hibernate = HibernatePersistenceStrategy.builder()
                .configuration(new AnnotationConfiguration()
                        .addAnnotatedClass(MultiModulesEntity.class)
                        .addProperties(Initializer.loadProperties("spt-persistence-multimodule1.properties")))
                .annotatedWith(MyUnit.class).build();
        Module hibernateModule = PersistenceService.using(hibernate)
                .across(UnitOfWork.TRANSACTION)
                .addAccessor(MultiModulesMyUnitAccessor.class)
                .buildModule();

        PersistenceStrategy hibernate2 = HibernatePersistenceStrategy.builder()
                .configuration(new AnnotationConfiguration()
                        .addAnnotatedClass(MultiModulesEntity.class)
                        .addProperties(Initializer.loadProperties("spt-persistence-multimodule2.properties")))
                .annotatedWith(MySecondUnit.class).build();
        Module hibernateModule2 = PersistenceService.using(hibernate2)
                .across(UnitOfWork.TRANSACTION)
                .addAccessor(MultiModulesMySecondUnitAccessor.class)
                .buildModule();

        injector = Guice.createInjector(hibernateModule, hibernateModule2,
                new PersistenceServiceExtrasModule());

        persistenceServices = injector.getInstance(Key.get(new TypeLiteral<List<PersistenceService>>() {
        }));

        // They create the schema, so starting them lazily cam cause data loss
        for (PersistenceService ps : persistenceServices) {
            ps.start();
        }
    }

    @AfterMethod
    public void tearDown() {
        for (PersistenceService ps : persistenceServices) {
            ps.shutdown();
        }
    }

    @Test
    public final void testDefaultTransactionsAppliedCorrectly() {
        PersistenceMate persistenceMate = injector.getInstance(PersistenceMate.class);
        persistenceMate.storeTwoEntitiesInMyUnit();
        persistenceMate.storeOneEntityInMySecondUnit();

        List<MultiModulesEntity> myUnitList = injector.getInstance(MultiModulesMyUnitAccessor.class).listAllWithMyUnit();
        List<MultiModulesEntity> mySecondUnitList = injector.getInstance(MultiModulesMySecondUnitAccessor.class).listAllWithMySecondUnit();

        Assert.assertEquals(myUnitList.size(), 2);
        Assert.assertEquals(mySecondUnitList.size(), 1);
    }

    static class PersistenceMate {
        @Inject
        @MyUnit
        Provider<Session> myUnitSession;
        @Inject
        @MySecondUnit
        Provider<Session> mySecondUnitSession;

        @Transactional(unit = MyUnit.class)
        public void storeTwoEntitiesInMyUnit() {
            try {
                mySecondUnitSession.get().createQuery("from MultiModulesEntity");
                Assert.fail("More than one transaction is active");
            } catch (Exception e) {
            }

            Session session = myUnitSession.get();

            MultiModulesEntity entity = new MultiModulesEntity();
            entity.setText("text1");
            session.persist(entity);

            MultiModulesEntity entity2 = new MultiModulesEntity();
            entity2.setText("text2");
            session.persist(entity2);
        }

        @Transactional(unit = MySecondUnit.class)
        public void storeOneEntityInMySecondUnit() {
            try {
                myUnitSession.get().createQuery("from MultiModulesEntity");
                Assert.fail("More than one transaction is active");
            } catch (Exception e) {
            }

            Session session = mySecondUnitSession.get();

            MultiModulesEntity entity = new MultiModulesEntity();
            entity.setText("text3");
            session.persist(entity);
        }
    }

    static class MultiModulesMyUnitAccessor {
        @Transactional(unit = MyUnit.class)
        @Finder(unit = MyUnit.class, query = "from MultiModulesEntity")
        public List<MultiModulesEntity> listAllWithMyUnit() {
            throw new AssertionError();
        }
    }

    static class MultiModulesMySecondUnitAccessor {
        @Transactional(unit = MySecondUnit.class)
        @Finder(unit = MySecondUnit.class, query = "from MultiModulesEntity")
        public List<MultiModulesEntity> listAllWithMySecondUnit() {
            throw new AssertionError();
        }
    }
}
