package com.wideplay.warp.db4o;

import java.io.IOException;
import java.util.Date;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.db4o.ObjectContainer;
import com.db4o.ObjectServer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.wideplay.warp.persist.PersistenceService;
import com.wideplay.warp.persist.TransactionStrategy;
import com.wideplay.warp.persist.Transactional;
import com.wideplay.warp.persist.UnitOfWork;

/**
 * 
 * @author Jeffrey Chung (lwbruce@gmail.com)
 */
@Test(suiteName = "db4o")
public class JoiningLocalTransactionsTest {
	private Injector injector;
	private static final String UNIQUE_TEXT = JoiningLocalTransactionsTest.class + "some unique text" + new Date();
    private static final String OTHER_UNIQUE_TEXT = JoiningLocalTransactionsTest.class + "some other unique text" + new Date();
    
    @BeforeClass
	public void preClass() {
		injector = Guice.createInjector(PersistenceService.usingDb4o()
				.across(UnitOfWork.TRANSACTION)
				.transactedWith(TransactionStrategy.LOCAL)
				.buildModule(),

				new AbstractModule() {
					protected void configure() {
						bindConstant().annotatedWith(Db4Objects.class).to("TestDatabase.data");
					}
				}
		);

		injector.getInstance(PersistenceService.class).start();
	}
    
    @AfterClass
	public void postClass() {
		injector.getInstance(ObjectServer.class).close();
	}
    
    @AfterTest
    public void postTest() {
    	ObjectServerHolder.closeCurrentObjectContainer();
    }
    
    @Test
    public void testSimpleTxn() {
    	injector.getInstance(JoiningLocalTransactionsTest.TransactionalObject.class).runInTxn();
    	
    	ObjectContainer oc = injector.getInstance(ObjectContainer.class);
    	
    	ObjectSet<Db4oTestObject> objSet = oc.query(new Predicate<Db4oTestObject>() {
	    		public boolean match(Db4oTestObject obj) {
	    			return obj.getText().equals(UNIQUE_TEXT);
	    		}
	    	});
    	ObjectServerHolder.closeCurrentObjectContainer();
    	
    	assert objSet.get(0) instanceof Db4oTestObject: "Odd result returned fatal";
    	assert objSet.get(0).getText().equals(UNIQUE_TEXT) : "Queried object did not match";
    }
    
    @Test
    public void testSimpleTxnRollbackOnChecked() {
    	try {
			injector.getInstance(JoiningLocalTransactionsTest.TransactionalObject.class).runInTxnThrowingChecked();
		} catch (IOException e) {
			System.out.println("Caught (expecting rollback): " + e);
		}
		
		ObjectContainer oc = injector.getInstance(ObjectContainer.class);
		
		ObjectSet<Db4oTestObject> objSet = oc.query(new Predicate<Db4oTestObject>() {
    		public boolean match(Db4oTestObject obj) {
    			return obj.getText().equals(OTHER_UNIQUE_TEXT);
    		}
    	});
		ObjectServerHolder.closeCurrentObjectContainer();
		
		assert objSet.isEmpty() : "Result was returned: rollback did not occur";
    }
    
    @Test
    public void testSimpleTxnRollbackOnUnchecked() {
    	try {
			injector.getInstance(JoiningLocalTransactionsTest.TransactionalObject.class).runInTxnThrowingUnchecked();
		} catch (RuntimeException e) {
			System.out.println("Caught (expecting rollback): " + e);
		}
		
		ObjectContainer oc = injector.getInstance(ObjectContainer.class);
		
		ObjectSet<Db4oTestObject> objSet = oc.query(new Predicate<Db4oTestObject>() {
    		public boolean match(Db4oTestObject obj) {
    			return obj.getText().equals(OTHER_UNIQUE_TEXT);
    		}
    	});
		ObjectServerHolder.closeCurrentObjectContainer();
		
		assert objSet.isEmpty() : "Result was returned: rollback did not occur";
    }
    
    public static class TransactionalObject {
    	private final ObjectContainer oc;
    	
    	@Inject
    	public TransactionalObject(ObjectContainer oc) {
    		this.oc = oc;
    	}
    	
    	@Transactional
    	public void runInTxn() {
    		runInTxnInternal();
    	}
    	
    	@Transactional(rollbackOn = IOException.class)
    	private void runInTxnInternal() {
    		Db4oTestObject obj = new Db4oTestObject(UNIQUE_TEXT);
    		oc.set(obj);
    	}
    	
    	@Transactional(rollbackOn = IOException.class)
    	public void runInTxnThrowingChecked() throws IOException {
    		runInTxnThrowingCheckedInternal();
    	}
    	
    	@Transactional
    	private void runInTxnThrowingCheckedInternal() throws IOException {
    		Db4oTestObject obj = new Db4oTestObject(OTHER_UNIQUE_TEXT);
    		oc.set(obj);
    		
    		throw new IOException();
    	}
    	
    	@Transactional
    	public void runInTxnThrowingUnchecked() {
    		runInTxnThrowingUncheckedInternal();
    	}
    	
    	@Transactional(rollbackOn = IOException.class)
    	private void runInTxnThrowingUncheckedInternal() {
    		Db4oTestObject obj = new Db4oTestObject(OTHER_UNIQUE_TEXT);
    		oc.set(obj);
    		
    		throw new IllegalStateException();
    	}
    }
}