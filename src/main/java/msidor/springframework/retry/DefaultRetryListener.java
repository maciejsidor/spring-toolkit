package msidor.springframework.retry;

import org.apache.log4j.Logger;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

public class DefaultRetryListener extends RetryListenerSupport
{
    private Logger log = Logger.getLogger(this.getClass());
    
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
