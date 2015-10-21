package msidor.springframework.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class CacheExpirationChecker 
{			
	private static final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<String, Long>();
			
	public static synchronized boolean check(String name,long expiration )
	{
		
		Logger.getLogger(CacheExpirationChecker.class).info("====> Checking chache "+name+" =====> Start");
		
		Long timestamp = timestamps.get(name);
		
		if(timestamp==null || timestamp==0)
		{
			timestamps.put(name,System.currentTimeMillis());
			
			Logger.getLogger(CacheExpirationChecker.class).info("====> Checking chache "+name+" =====> Stop on true");
			return true;			
		}
		else if(System.currentTimeMillis()-timestamp>expiration)
		{
			timestamps.put(name,System.currentTimeMillis());
			
			Logger.getLogger(CacheExpirationChecker.class).info("====> Checking chache "+name+" =====> Stop on true");
			return true;
		}
		
		
		Logger.getLogger(CacheExpirationChecker.class).info("====> Checking chache "+name+" =====> Stop on false");
		return false;
	}


	
}
