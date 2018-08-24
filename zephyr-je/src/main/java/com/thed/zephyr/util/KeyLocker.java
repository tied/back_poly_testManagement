package com.thed.zephyr.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A utility class to manage a set of locks. Each lock is identified by a String
 * which serves as a key. Typical usage is: class Example{ private final static
 * KeyLocker locker = new Locker();
 * 
 * public void foo(String s){ Lock lock = locker.acquireLock(s); try { //
 * whatever }finally{ lock.unlock(); } } }
 * 
 */
public class KeyLocker {
	private static final Log LOG = LogFactory.getLog(KeyLocker.class);

	// We need an atomic counter to manage the number of users using the lock
	// and free it when
	// it's equal to zero.
	private final Map<String, ReentrantLock> locks = Collections.synchronizedMap(new HashMap<String, ReentrantLock>());
//	private final List<Long> inProgressIssues = Collections.synchronizedList(new ArrayList<Long>());
//	private final Condition inProgressCondition = lock.newCondition();
	

	/**
	 * Return a lock for the given key. The lock is already locked.
	 * 
	 * @param key
	 * @throws InterruptedException 
	 */
	public Boolean acquireLock(String lastTwo) throws InterruptedException {
		ReentrantLock lock = locks.get(lastTwo);
		if(lock == null){
			lock = new ReentrantLock();
			locks.put(lastTwo, lock);
		}
		lock.lock();
		
//		while(!gotLock){
//			if(inProgressIssues.contains(issueId)){
//				inProgressCondition.await();
//			}
//			synchronized(inProgressIssues){
//				if(!inProgressIssues.contains(issueId))
//					gotLock = inProgressIssues.add(issueId);
//			}
//		}
		return true;
	}

	/**
	 * Free the lock for the given key.
	 */
	public Boolean releaseLock(String lastTwo) {
		ReentrantLock lock = locks.get(lastTwo);
		if(lock != null && lock.isHeldByCurrentThread()){
			lock.unlock();
			return true;
		}
//		synchronized(inProgressIssues){
//			if(inProgressIssues.contains(issueId)){
//				inProgressIssues.remove(issueId);
				//inProgressCondition.signal();
//				lock.unlock();
//				return true;
//			}
//		}
		return false;
	}
}
