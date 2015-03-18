package code.core.npc.rpc.protocol;

import java.lang.reflect.Type;

/**
 * Decoder Interface
 * 
 */
public interface Decoder {

	/**
	 * decode byte[] to Object
	 * @param paramTypes 
	 */
	public Object decode(String className, Type paramTypes, byte[] bytes) throws Exception;
	
}
