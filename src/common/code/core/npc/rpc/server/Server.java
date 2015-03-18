package code.core.npc.rpc.server;
/**
 *   
 */
import java.util.concurrent.ExecutorService;
/**
 * 
 */
public interface Server {

	/**
	 * start server at listenPort,requests will be handled in businessThreadPool
	 */
	public void start(int listenPort,ExecutorService businessThreadPool) throws Exception;
	
	/**
	 * register business handler
	 */
	public void registerProcessor(int protocolType,String serviceName,Object serviceInstance, Class<?> iface);
	
	/**
	 * stop server
	 */
	public void stop() throws Exception;
	
}
