package com.googlecode.msidor.springframework.cache;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

public class TimeEvictBlockingCache implements MethodInterceptor
{
	private Object cacheObject = null;
	
	private final Logger log = Logger.getLogger(this.getClass()); 
	
	private final ReentrantReadWriteLock 	lock 						= new ReentrantReadWriteLock();	
	private final Lock 						readLock 					= lock.readLock();	
	private final ReentrantLock				reserveWritelock 			= new ReentrantLock();
	private final Lock 						writeLock 					= lock.writeLock();
	private final ReentrantLock				initializationlock 			= new ReentrantLock();
	private final Condition					initializatiionCondition	= this.initializationlock.newCondition();
	
		
	private long lastCacheUpdate = 0;
	private long cacheExpirationTime = 0;
	
	public long getCacheExpirationTime() 
	{
		return cacheExpirationTime;
	}

	public void setCacheExpirationTime(long cacheExpirationTime) 
	{
		this.cacheExpirationTime = cacheExpirationTime;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable 
    {
			log.trace("Intercepting "+invocation.getMethod().toGenericString());
		
			boolean hasReadLock 				= false;
			boolean hasWriteLock 				= false;
			boolean hasReservedWritelock 		= false;
			boolean hasInitializationLock		= false;
			
			Exception exceptionToThrow 			= null;
			
			Object cachedObjectToReturn  		= null;
		   		
		    try
		    {
    	
		       readLock.lock();
		       hasReadLock = true;
		       log.trace("Taking read lock");
		       
		       if(lastCacheUpdate==0 || System.currentTimeMillis()-lastCacheUpdate>cacheExpirationTime)
		       {	    		   	    		   	    		   		    	   		    	   
		    	   if(reserveWritelock.tryLock())
		    	   {
		    		   log.trace("Taking update lock");
		    		   
		    		   hasReservedWritelock = true;
		    	   
			    	   readLock.unlock();	
			    	   hasReadLock=false;
			    	   log.trace("Releasing read lock");
			    	   
			    	   writeLock.lock();		    	   
			    	   hasWriteLock = true;
			    	   
			    	   log.trace("Taking write lock");
		     		   log.trace("Refreshing cache");
		     		   		     		   	    		   	    		   	    		   	    		 
		     		   cacheObject = invocation.proceed();
		     		   
		    		   if(lastCacheUpdate==0)
		    		   {		    			   		    			  
			     		   initializationlock.lock();
			     		   hasInitializationLock=true;
			     		   log.trace("Taking initialization lock and notifying all readers");
			     		   
			     		   initializatiionCondition.signalAll();
			     		   
			     		   initializationlock.unlock();
			     		   hasInitializationLock=false;			
			     		   log.trace("Releasing initalization lock");
			     		  
		    		   }
		     		   
		     		   lastCacheUpdate=System.currentTimeMillis();
		    	   }
		    	   else
		    	   {
		    		   if(lastCacheUpdate==0)
		    		   {		    			   		    			   
		    			   initializationlock.lock();		    			   
		    			   hasInitializationLock=true;
		    			   log.trace("Taking initialization lock and awaiting for notify");
		    			   
				    	   readLock.unlock();	
				    	   hasReadLock=false;		    
				    	   
		    			   initializatiionCondition.await();
		    			   
		    			   log.trace("Awaking from initialization");
		    			   
			     		   initializationlock.unlock();
			     		   hasInitializationLock=false;			     		   
			     		   log.trace("Releasing initalization lock");
		    			   
				    	   readLock.lock();	
				    	   hasReadLock=true;
				    	   log.trace("Retaking read lock");
				    	   
		    			   
		    			   
		    		   }
		    	   }
		    
		       }	
		       
		       cachedObjectToReturn = cacheObject;
		       
	    	
		    }
		    catch (Exception e)
		    {
		       exceptionToThrow = e;
		    }
		    finally
		    {
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
		       
		       if(exceptionToThrow!=null)
		       {
		    	   
		    	   log.trace("Exiting on exception");
		    	   throw exceptionToThrow;
		       }
		    }



		    log.trace("Returning result");
		    return cachedObjectToReturn;		
	       
	       

	       
	       

    }


}
