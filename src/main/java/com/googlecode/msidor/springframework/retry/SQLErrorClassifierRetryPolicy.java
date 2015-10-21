package com.googlecode.msidor.springframework.retry;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.classify.Classifier;
import org.springframework.classify.ClassifierSupport;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.util.Assert;

import com.googlecode.msidor.springframework.classify.RangeClassifier;

public class SQLErrorClassifierRetryPolicy implements RetryPolicy 
{

    private Classifier<String, RetryPolicy> exceptionClassifier = new ClassifierSupport<String, RetryPolicy>(new NeverRetryPolicy() );

    /**
     * Setter for policy map used to create a classifier. Either this property
     * or the exception classifier directly should be set, but not both.
     *
     * @param policyMap a map of Throwable class to {@link RetryPolicy} that
     * will be used to create a {@link Classifier} to locate a policy.
     */
    public void setPolicyMap( Map<String, RetryPolicy> policyMap )
    {
        this.exceptionClassifier = new RangeClassifier<RetryPolicy>( policyMap);
    }

    /**
     * Setter for an exception classifier. The classifier is responsible for
     * translating exceptions to concrete retry policies. Either this property
     * or the policy map should be used, but not both.
     *
     * @param exceptionClassifier ExceptionClassifier to use
     */
    public void setExceptionClassifier( Classifier<String, RetryPolicy> exceptionClassifier )
    {
        this.exceptionClassifier = exceptionClassifier;
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

	public RetryContext open(RetryContext parent) 
	{
		return new ExceptionClassifierRetryContext( parent, exceptionClassifier).open( parent );
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
    private static class ExceptionClassifierRetryContext
        extends RetryContextSupport
        implements RetryPolicy
    {

        final private Classifier<String, RetryPolicy> exceptionClassifier;

        // Dynamic: depends on the latest exception:
        private RetryPolicy policy;

        // Dynamic: depends on the policy:
        private RetryContext context;

        final private Map<RetryPolicy, RetryContext> contexts = new HashMap<RetryPolicy, RetryContext>();

        public ExceptionClassifierRetryContext( RetryContext parent, Classifier<String, RetryPolicy> exceptionClassifier)
        {
            super( parent );
            this.exceptionClassifier = exceptionClassifier;
        }

        public boolean canRetry( RetryContext context )
        {
            return this.context == null || policy.canRetry( this.context );
        }

        public void close( RetryContext context )
        {
            // Only close those policies that have been used (opened):
            for ( RetryPolicy policy : contexts.keySet() )
            {
                policy.close( getContext( policy, context.getParent() ) );
            }
        }

        public RetryContext open( RetryContext parent )
        {
            return this;
        }

        public void registerThrowable( RetryContext context, Throwable throwable )
        {
        	if(throwable instanceof SQLException)
        	{
        		SQLException sqlException = (SQLException)throwable;
        		policy = exceptionClassifier.classify( ""+sqlException.getErrorCode());
        	}
        	else if(throwable.getCause() instanceof SQLException)
        	{
        		SQLException sqlException = (SQLException)throwable.getCause();
        		policy = exceptionClassifier.classify( ""+sqlException.getErrorCode());
        	}
        	else 
        	{
        		policy = new NeverRetryPolicy();
        	}
            
            Assert.notNull( policy, "Could not locate policy for exception=[" + throwable + "]." );
            this.context = getContext( policy, context.getParent() );
            policy.registerThrowable( this.context, throwable );
        }

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
