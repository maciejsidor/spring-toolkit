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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.classify.Classifier;
import org.springframework.classify.ClassifierSupport;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.util.Assert;

import com.googlecode.msidor.springframework.classify.SpELExpressionsClassifier;
import com.googlecode.msidor.springframework.classify.SpELExpressionsClassifierConfig;

/**
 * Class that provides retry policy based on the SpEL expression evaluation on exception object. 
 * 
 * This class is based on {@link ExceptionClassifierRetryPolicy}. 
 *  
 * @see SpELExpressionsClassifierConfig
 * @see SpELExpressionsClassifier
 * @author Maciej SIDOR (maciejsidor@gmail.com)
 * @since 2015
 */
public class SpELExpressionsClassifierRetryPolicy implements RetryPolicy 
{

	/**
	 * Default clarifier that always return NeverRetryPolicy
	 */
    private Classifier<Throwable, RetryPolicy> exceptionClassifier = new ClassifierSupport<Throwable, RetryPolicy>(new NeverRetryPolicy() );

    /**
     * Setter for policy list used to create a SpELExpressionsClassifier classifier.
     *
     * @param policyMap a list of "expression to value" mappings ({@link SpELExpressionsClassifierConfig})
     * that will be used to create a {@link SpELExpressionsClassifier} to locate a policy.
     */
    public void setPolicies( List<SpELExpressionsClassifierConfig<RetryPolicy>> policies )
    {
        this.exceptionClassifier = new SpELExpressionsClassifier<Throwable,RetryPolicy>( policies);
    }


    /**
     * Delegate to the policy currently activated in the context.
     *
     * @see org.springframework.retry.RetryPolicy#canRetry(org.springframework.retry.RetryContext)
     */
    public boolean canRetry( RetryContext context )
    {
        RetryPolicy policy = (RetryPolicy) context;
        return policy.canRetry( context );
    }

    /**
     * Delegate to the policy currently activated in the context.
     *
     * @see org.springframework.retry.RetryPolicy#close(org.springframework.retry.RetryContext)
     */
    public void close( RetryContext context )
    {
        RetryPolicy policy = (RetryPolicy) context;
        policy.close( context );
    }

    /**
     * Opens the retry context
     * 
     * @param context of calling policy
     * @return new context instance
     */
	public RetryContext open(RetryContext parent) 
	{
		return new SpELExceptionClassifierRetryContext( parent, exceptionClassifier).open( parent );
	}

    /**
     * Delegate to the policy currently activated in the context.
     *
     * @see org.springframework.retry.RetryPolicy#registerThrowable(org.springframework.retry.RetryContext,
     * Throwable)
     */
    public void registerThrowable( RetryContext context, Throwable throwable )
    {
        RetryPolicy policy = (RetryPolicy) context;
        policy.registerThrowable( context, throwable );
        ( (RetryContextSupport) context ).registerThrowable( throwable );
    }
    
    @SuppressWarnings("serial")
    /**
     * Inner class that is instantiated on each retry 
     * 
	 * @author Maciej SIDOR (maciejsidor@gmail.com)
	 * @since 2015
     */
    private static class SpELExceptionClassifierRetryContext
        extends RetryContextSupport
        implements RetryPolicy
    {

    	/**
    	 * Internal classifier 
    	 */
        final private Classifier<Throwable, RetryPolicy> exceptionClassifier;

        /**
         * Dynamic: depends on the latest exception:
         */
        private RetryPolicy policy;

        /**
         * Dynamic: depends on the policy:
         */
        private RetryContext context;

        /**
         * Contexts store
         */
        final private Map<RetryPolicy, RetryContext> contexts = new HashMap<RetryPolicy, RetryContext>();

        /**
         * Default constructor
         * 
         * @param parent currently activated context
         * @param exceptionClassifier classifier that will be used to locate the policy
         */
        public SpELExceptionClassifierRetryContext( RetryContext parent, Classifier<Throwable, RetryPolicy> exceptionClassifier)
        {
            super( parent );
            this.exceptionClassifier = exceptionClassifier;
        }

        /**
         * Check if policy allows retry
         */
        public boolean canRetry( RetryContext context )
        {
            return this.context == null || policy.canRetry( this.context );
        }

        /**
         * Close all policies registered for context
         * @param context context to close the policies for
         */
        public void close( RetryContext context )
        {
            // Only close those policies that have been used (opened):
            for ( RetryPolicy policy : contexts.keySet() )
            {
                policy.close( getContext( policy, context.getParent() ) );
            }
        }

        /**
         * Returns this instance
         */
        public RetryContext open( RetryContext parent )
        {
            return this;
        }

        /**
         * Locates and sets the policy by using classifier and delegates to that policy
         * 
         * @param context the retry context
         * @param throwable the throwable that triggered the retry 
         *
         */
        public void registerThrowable( RetryContext context, Throwable throwable )
        {

    		policy = exceptionClassifier.classify(throwable);

            Assert.notNull( policy, "Could not locate policy for exception=[" + throwable + "]." );
            this.context = getContext( policy, context.getParent() );
            policy.registerThrowable( this.context, throwable );
        }

        /**
         * Returns context from store for given policy or registers the context into store for given policy  
         * 
         * @param policy to find the context for
         * @param parent to use/register if no context has yet been set for given policy
         * @return context for policy
         */
        private RetryContext getContext( RetryPolicy policy, RetryContext parent )
        {
            RetryContext context = contexts.get( policy );
            if ( context == null )
            {
                context = policy.open( parent );
                contexts.put( policy, context );
            }
            return context;
        }

    }    

}
