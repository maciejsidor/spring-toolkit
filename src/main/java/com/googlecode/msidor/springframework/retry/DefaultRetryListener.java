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
package com.googlecode.msidor.springframework.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

/**
 * Provides basic logging for retry events
 *  
 *
 * @author Maciej SIDOR (maciejsidor@gmail.com)
 * @since 2015
 */
public class DefaultRetryListener extends RetryListenerSupport
{
	private final Logger log = LoggerFactory.getLogger(this.getClass());
    
	/**
	 * Called after every unsuccessful attempt at a retry.
	 * 
	 * @param context the current {@link RetryContext}.
	 * @param callback the current {@link RetryCallback}.
	 * @param throwable the last exception that was thrown by the callback.
	 */
    @Override
    public <T, E extends Throwable> void onError( RetryContext context, RetryCallback<T,E> callback,Throwable throwable )
    {
        try
        {
            log.trace( "Retrying on exception "+throwable.getClass().getCanonicalName()+"@"+throwable.getMessage()+" [attempt "+ (context.getRetryCount()+1)+"]" );
        }
        catch(Exception e)
        {
            log.trace( "Retrying on exception");
        }
        
        super.onError( context, callback, throwable );
    }
    
	/**
	 * Called after the final attempt (successful or not). Allow the interceptor
	 * to clean up any resource it is holding before control returns to the
	 * retry caller.
	 * 
	 * @param context the current {@link RetryContext}.
	 * @param callback the current {@link RetryCallback}.
	 * @param throwable the last exception that was thrown by the callback.
	 */    
    @Override
    public <T, E extends Throwable> void close( RetryContext context, RetryCallback<T,E> callback, Throwable throwable )
    {
        try
        {
            if (context.getRetryCount()>0)
            {
            
                if(throwable!=null)
                {
                    log.trace( "Failed on retrying on exception "+throwable.getClass().getCanonicalName() +" [total attempts "+(context.getRetryCount())+"]" );
                }
                else
                {
                    log.trace( "Succeeded on retrying on exception");
                }
            }
        }
        catch(Exception e)
        {
            log.trace( "Failed on retrying on exception");
        }            
        
        super.close( context, callback, throwable );
    }
    
    

}
