package code.core.npc.rpc.protocol;
/**
 *   
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RPCProtocol implements Protocol {
	
	public static final int TYPE = 1;
	
	private static final Log LOGGER = LogFactory.getLog(RPCProtocol.class);
	
	public ByteBufferWrapper encode(Object message,ByteBufferWrapper bytebufferWrapper) throws Exception{
		return null;
	}
	
	public Object decode(ByteBufferWrapper wrapper,Object errorObject,int...originPosArray) throws Exception{
		return null;
	}

}
