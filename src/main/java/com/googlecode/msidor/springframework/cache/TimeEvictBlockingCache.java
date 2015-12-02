/*
Copyright 2015 Maciej SIDOR [maciejsidor@gmail.com]

The source code is licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.	
 */
package com.googlecode.msidor.springframework.cache;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple time based evict blocking cache implementation.
 * <br/>
 * This cache is in fact a method intercepter that cache method result fora given time</br>
 * 
 * "simple" means that this cache implementation can store only one object and there is no support for method parameters to keys mapping.
 * As for the keys support: the cached object may be for instance a map of objects but it is important to note that the keys will be handled directly by the map implementation.
 * It also means that if cached object is released all map values are gone as well.
 * <br/>
 *   
 * "time based evict" means that cache content is evicted based on expiration time.
 * <br/>
 * 
 * "blocking" means that threads accessing cache content are blocked when cache is being reloaded (invoking proxied method). 
 * Accessing object's properties is not blocking. 
 * It is to cached object implementation to assure concurrency compliance.<br/>
 * 
 * Blocking strategy may be described by 3 cases:
 * <ul>
 * <li>initialization - first thread reading data will be granted the permission to load cache object (by invoking proxied method). All other threads will be blocked waiting for data.</li>
 * <li>read - all threads can access simultaneously the cached object for read.</li>
 * <li>reload - when a thread finds out that cache object is expired it will wait for all reading threads to finish. New reading threads will block until cached object is reloaded.</li>
 * </ul>
 * <br/> 
 * 
 *
 * @author Maciej SIDOR (maciejsidor@gmail.com)
 * @since 2015
 */
public class TimeEvictBlockingCache implements MethodInterceptor
{
	/**
	 * The cached object
	 */
	private Object cacheObject = null;
	
	/**
	 * The cache logger
	 */
	private final Logger log = LoggerFactory.getLogger(this.getClass()); 
	
	/**
	 * Read/Write lock
	 */
	private final ReentrantReadWriteLock 	lock 						= new ReentrantReadWriteLock();
	
	/**
	 * Lock for read
	 */
	private final Lock 						readLock 					= lock.readLock();	
	
	/**
	 * Lock for write
	 */
	private final Lock 						writeLock 					= lock.writeLock();
	
	/**
	 * Intermediate lock to limit passing to write mode to only by one thread 
	 */
	private final ReentrantLock				reserveWritelock 			= new ReentrantLock();
	
	/**
	 * Intermediate lock that holds all reading threads until cache date are initialized 
	 */
	private final ReentrantLock				initializationlock 			= new ReentrantLock();
	
	/**
	 * Condition for initialization lock
	 */
	private final Condition					initializatiionCondition	= this.initializationlock.newCondition();
	
		
	/**
	 * Stores cache last update time
	 */
	private long lastCacheUpdate = 0;
	
	/**
	 * Time after which the cached object will be renewed
	 */
	private long cacheExpirationTime = 0;
	
	/**
	 * @return Time after which the cached object will be renewed
	 */
	public long getCacheExpirationTime() 
	{
		return cacheExpirationTime;
	}

	/**
	 * Sets time after which the cached object will be renewed
	 * @param cacheExpirationTime
	 */
	public void setCacheExpirationTime(long cacheExpirationTime) 
	{
		this.cacheExpirationTime = cacheExpirationTime;
	}

	/**
	 * Method call interception. 
	 * Whole cache implementation happens here.
	 * 
	 * @param method call parameters
	 * @return result of proxied method either from cache or directly from method call 
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable 
    {
			log.trace("Intercepting "+invocation.getMethod().toGenericString());
		
			/*variables to keep the lock states*/
			boolean hasReadLock 				= false;
			boolean hasWriteLock 				= false;
			boolean hasReservedWritelock 		= false;
			boolean hasInitializationLock		= false;
			
			/*intercepted exception to rethrow after releasing locks*/
			Exception exceptionToThrow 			= null;
			
			/*Cached object to be returned*/
			Object cachedObjectToReturn  		= null;
		   		
		    try
		    {
    	
		       /*take the read lock by default*/
		       readLock.lock();		       
		       hasReadLock = true;
		       log.trace("Taking read lock");
		       
		       /*if cached object was not set yet or is expired  */
		       if(lastCacheUpdate==0 || System.currentTimeMillis()-lastCacheUpdate>cacheExpirationTime)
		       {	    		   	    		   	    		
		    	   /*try to take intermediate write lock - only one thread can do that*/
		    	   if(reserveWritelock.tryLock())
		    	   {		    		   
		    		   log.trace("Taking update lock");		    		   
		    		   hasReservedWritelock = true;
		    		   
		    		   
		    		   /*
		    		    * Direct upgrade from read to write lock is not possible.
		    		    * First a thread must drop its read lock to go to write lock
		    		    */
		    		   		    	  
			    	   readLock.unlock();	
			    	   hasReadLock=false;
			    	   log.trace("Releasing read lock");
			    	   			    	   
			    	   writeLock.lock();		    	   
			    	   hasWriteLock = true;			    	   			    	   
			    	   log.trace("Taking write lock");
			    	   
			    	   /*now read threads are no more accepted*/
			    	   
		     		   /*call intercepted method*/
		     		   log.trace("Refreshing cache");		     		   		
		     		   cacheObject = invocation.proceed();
		     		   
		     		   /*in case of first interception*/
		    		   if(lastCacheUpdate==0)
		    		   {		    			   		
		    			   /*take initialization lock*/
			     		   initializationlock.lock();
			     		   hasInitializationLock=true;
			     		   log.trace("Taking initialization lock and notifying all readers");
			     		   
			     		   /*notify all threads that waits on initialization condition*/
			     		   initializatiionCondition.signalAll();
			     		   
			     		   /*release initialization lock to allow waiting threads to proceed*/
			     		   initializationlock.unlock();
			     		   hasInitializationLock=false;			
			     		   log.trace("Releasing initalization lock");
			     		  
		    		   }
		     		   
		    		   /*update last cache update time*/
		     		   lastCacheUpdate=System.currentTimeMillis();
		    	   }
		    	   else /*when thread could not get the intermediate write lock*/
		    	   {
		    		   
		    		   /*if this is the first method interception and thus the cached object is not there yet*/
		    		   if(lastCacheUpdate==0)
		    		   {		   
		    			   /*take initialization lock*/
		    			   initializationlock.lock();		    			   
		    			   hasInitializationLock=true;
		    			   log.trace("Taking initialization lock and awaiting for notify");
		    			   
		    			   /*
		    			    * Drop the read lock in order to allow thread with write lock to proceed.
		    			    * Otherwise threads will be deadlocking each others.
		    			    * */
				    	   readLock.unlock();	
				    	   hasReadLock=false;		    
				    	   
				    	   /*wait for notification from writer thread*/
		    			   initializatiionCondition.await();
		    			   
		    			   log.trace("Awaking from initialization wait");
		    			   
		    			   /*unlock initialization lock */
			     		   initializationlock.unlock();
			     		   hasInitializationLock=false;			     		   
			     		   log.trace("Releasing initalization lock");
		    			   
			     		   
			     		   /*retake read lock*/
				    	   readLock.lock();	
				    	   hasReadLock=true;
				    	   log.trace("Retaking read lock");
				    	   
		    			   
		    			   
		    		   }
		    	   }
		    
		       }	
		       
		       /*set cached object - most of time only this part of code is executed*/
		       cachedObjectToReturn = cacheObject;
		       
	    	
		    }
		    catch (Exception e)
		    {
		       /*intercept thrown exception*/
		       exceptionToThrow = e;
		    }
		    finally
		    {
		       /*release all taken locks*/	
		    	
		       if(hasWriteLock)	  
		       {
		    	   log.trace("Releasing write lock");
		    	   writeLock.unlock();
		       }
		       
		       if(hasReadLock)
		       {
		    	   log.trace("Releasing read lock");
		    	   readLock.unlock();
		       }
		       
		       if(hasReservedWritelock)
		       {
		    	   log.trace("Releasing update lock");
		    	   reserveWritelock.unlock();
		       }
		       
		       if(hasInitializationLock)
		       {
		    	   log.trace("Releasing initalization lock");
		    	   initializationlock.unlock();
		       }
		       
		       /*rethrow intercepted exception*/
		       if(exceptionToThrow!=null)
		       {
		    	   
		    	   log.trace("Exiting on exception");
		    	   throw exceptionToThrow;
		       }
		    }

		    /*return cached object*/
		    log.trace("Returning result");
		    return cachedObjectToReturn;		
    }


}
