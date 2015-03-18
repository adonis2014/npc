package code.core.npc.rpc.client;

/**
 *   
 */
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import code.core.npc.rpc.HandshakeRequest;
import code.core.npc.rpc.HandshakeResponse;
import code.core.npc.rpc.NettyDataPack;
import code.core.npc.rpc.utils.ArvoUtils;

/**
 * Common Client,support sync invoke
 * 
 */
public abstract class AbstractClient implements Client {

	private static final Log LOGGER = LogFactory.getLog(AbstractClient.class);

	private static final boolean isDebugEnabled = LOGGER.isDebugEnabled();
	
	private static final boolean isWarnEnabled = LOGGER.isWarnEnabled();

	private static final long PRINT_CONSUME_MINTIME = Long.parseLong(System
			.getProperty("code.core.print.consumetime", "0"));
	
	private static final SpecificDatumReader<HandshakeResponse> HANDSHAKE_READER =
		    new SpecificDatumReader<HandshakeResponse>(HandshakeResponse.class);

	protected static ConcurrentHashMap<Integer, ArrayBlockingQueue<Object>> responses = 
			new ConcurrentHashMap<Integer, ArrayBlockingQueue<Object>>();
	
	public Object invokeSync(Object message, int timeout, int codecType, int protocolType)
			throws Exception {
		return invokeSyncIntern(null, null, null, null);
	}

	public Object invokeSync(String targetInstanceName, Method method, String methodName,
			String[] argTypes, Object[] args, int timeout, int codecType, int protocolType, Class<?> iface)
			throws Exception {
		byte[][] argTypeBytes = new byte[argTypes.length][];
		for(int i =0; i < argTypes.length; i++) {
		    argTypeBytes[i] =  argTypes[i].getBytes();
		}
		
		HandshakeRequest request = new HandshakeRequest(timeout, protocolType, targetInstanceName, methodName);
		return invokeSyncIntern(request, args, method, iface);
	}

	private Object invokeSyncIntern(HandshakeRequest wrapper, Object[] args, Method method, Class<?> iface) throws Exception {
		long beginTime = System.currentTimeMillis();
		ArrayBlockingQueue<Object> responseQueue = new ArrayBlockingQueue<Object>(1);
		responses.put(wrapper.getId(), responseQueue);
		NettyDataPack responseWrapper = null;
		try {
			if(isDebugEnabled){
				// for performance trace
				LOGGER.debug("client ready to send message,request id: "+wrapper.getId());
			}
			getClientFactory().checkSendLimit();
			sendRequest(wrapper, args, wrapper.getTimeout(), iface);
			if(isDebugEnabled){
				// for performance trace
				LOGGER.debug("client write message to send buffer,wait for response,request id: "+wrapper.getId());
			}
		} 
		catch (Exception e) {
			responses.remove(wrapper.getId());
			responseQueue = null;
			LOGGER.error("send request to os sendbuffer error", e);
			throw e;
		}
		Object result = null;
		try {
			result = responseQueue.poll(
					wrapper.getTimeout() - (System.currentTimeMillis() - beginTime),
					TimeUnit.MILLISECONDS);
		}
		catch(Exception e){
			responses.remove(wrapper.getId());
			LOGGER.error("Get response error", e);
			throw new Exception("Get response error", e);
		}
		responses.remove(wrapper.getId());
		
		if (PRINT_CONSUME_MINTIME > 0 && isWarnEnabled) {
			long consumeTime = System.currentTimeMillis() - beginTime;
			if (consumeTime > PRINT_CONSUME_MINTIME) {
				LOGGER.warn("client.invokeSync consume time: "
						+ consumeTime + " ms, server is: " + getServerIP()
						+ ":" + getServerPort() + " request id is:"
						+ wrapper.getId());
			}
		}
		if (result == null) {
			String errorMsg = "receive response timeout("
					+ wrapper.getTimeout() + " ms),server is: "
					+ getServerIP() + ":" + getServerPort()
					+ " request id is:" + wrapper.getId();
			throw new Exception(errorMsg);
		}
		
		if(result instanceof NettyDataPack){
			responseWrapper = (NettyDataPack) result;
		}
		else if(result instanceof List){
			@SuppressWarnings("unchecked")
			List<NettyDataPack> responseWrappers = (List<NettyDataPack>) result;
			for (NettyDataPack response : responseWrappers) {
				if(response.getSerial() == wrapper.getId()){
					responseWrapper = response;
				}
				else{
					putResponse(response);
				}
			}
		}
		else{
			throw new Exception("only receive ResponseWrapper or List as response");
		}
		
		ByteBufferInputStream bbi = new ByteBufferInputStream(responseWrapper.getDatas());
	    BinaryDecoder in = DecoderFactory.get().binaryDecoder(bbi, null);
	    boolean isError = false;
	    Message message = null;
	    Object responseObject = null;
	    GenericData data = null;
		try{
			HandshakeResponse handshake = HANDSHAKE_READER.read(null, in);
			handshake.getRequestId();
			
			isError = in.readBoolean();
			
			Protocol protocol = getProtocol(iface);
			
			message = protocol.getMessages().get(method.getName());
			data = getGenericData(iface);
			
			if(!isError){
				responseObject = getDatumReader(message.getResponse(), data).read(null, in);
			}
		}
		catch(Exception e){
			LOGGER.error("Deserialize response object error", e);
			throw new Exception("Deserialize response object error", e);
		}
		if (isError) {
			Exception error = readError(message.getErrors(), data, in);
			String errorMsg = "server error,server is: " + getServerIP()
					+ ":" + getServerPort() + " request id is:"
					+ wrapper.getId();
			LOGGER.error(errorMsg, error);
	        throw error;
		}
		return responseObject;
	}
	
	private Exception readError(Schema reader, GenericData data, Decoder in)
		    throws IOException {
		    Object value = getDatumReader(reader, data).read(null, in);
		    if (value instanceof Exception)
		      return (Exception)value;
		    return new Exception(value.toString());
		  }
	
	private DatumReader<Object> getDatumReader(Schema reader, GenericData data) {
	    return new ReflectDatumReader<Object>(reader, reader, (ReflectData) data);
	  }
	
	protected Protocol getProtocol(Class<?> iface) {
		return ArvoUtils.getProtocol(iface);
//		return ((ReflectData) getGenericData(iface)).getProtocol(iface);
	}
	
	protected GenericData getGenericData(Class<?> iface){
		return new ReflectData(iface.getClassLoader());
	}

	/**
	 * receive response
	 */
	public void putResponse(NettyDataPack wrapper) throws Exception {
		if (!responses.containsKey(wrapper.getSerial())) {
			LOGGER.warn("give up the response,request id is:" + wrapper.getSerial() + ",maybe because timeout!");
			return;
		}
		try {
			ArrayBlockingQueue<Object> queue = responses.get(wrapper.getSerial());
			if (queue != null) {
				queue.put(wrapper);
			} 
			else {
				LOGGER.warn("give up the response,request id is:"
						+ wrapper.getSerial() + ",because queue is null");
			}
		} 
		catch (InterruptedException e) {
			LOGGER.error("put response error,request id is:" + wrapper.getSerial(), e);
		}
	}
	
	/**
	 * receive responses
	 */
	public void putResponses(List<NettyDataPack> wrappers) throws Exception {
		for (NettyDataPack wrapper : wrappers) {
			if (!responses.containsKey(wrapper.getSerial())) {
				LOGGER.warn("give up the response,request id is:" + wrapper.getSerial() + ",maybe because timeout!");
				continue;
			}
			try {
				ArrayBlockingQueue<Object> queue = responses.get(wrapper.getSerial());
				if (queue != null) {
					queue.put(wrappers);
					break;
				} 
				else {
					LOGGER.warn("give up the response,request id is:"
							+ wrapper.getSerial() + ",because queue is null");
				}
			} 
			catch (InterruptedException e) {
				LOGGER.error("put response error,request id is:" + wrapper.getSerial(), e);
			}
		}
	}

	/**
	 * send request to os sendbuffer,must ensure write result
	 */
	public abstract void sendRequest(HandshakeRequest wrapper, Object[] args, int timeout, Class<?> iface) throws Exception; 

}
