package code.core.npc.rpc.server;
/**
 * 
 */
public interface ServerProcessor {

	/**
	 * Handle request,then return Object
	 * 
	 * @param request
	 * @return Object
	 * @throws Exception
	 */
	public Object handle(Object request) throws Exception;
	
}
