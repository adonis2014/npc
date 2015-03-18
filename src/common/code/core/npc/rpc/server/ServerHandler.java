package code.core.npc.rpc.server;
/**
 *   
 */
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;

import code.core.npc.rpc.HandshakeRequest;
/**
 * Server Handler interface,when server receive message,it will handle 
 * 
 */
public interface ServerHandler {

	/**
	 * register business handler,provide for Server
	 */
	public void registerProcessor(String instanceName, Object instance, Class<?> iface);

	/**
	 * handle the request
	 */
	public void handleRequest(final HandshakeRequest request, Decoder in, BinaryEncoder out) throws Exception;
	
	public GenericData getGenericData();

}