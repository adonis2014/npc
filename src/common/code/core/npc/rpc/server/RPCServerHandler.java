package code.core.npc.rpc.server;

/**
 *   
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.Protocol;
import org.apache.avro.Protocol.Message;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.UnresolvedUnionException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import code.core.npc.rpc.HandshakeRequest;
import code.core.npc.rpc.utils.ArvoUtils;

/**
 * Reflection RPC Server Handler
 * 
 */
public class RPCServerHandler implements ServerHandler {

	private static final Log LOGGER = LogFactory.getLog(RPCServerHandler.class);

	// Server Processors key: servicename value: service instance
	private static Map<String, ProcessData> processors = new HashMap<String, ProcessData>();

	// Cached Server Methods key: instanceName#methodname$argtype_argtype
	private static Map<String, Method> cacheMethods = new HashMap<String, Method>();

	private GenericData data;
	
	public void registerProcessor(String instanceName, Object instance,
			Class<?> iface) {

		processors.put(instanceName, new ProcessData(instance, iface));
		Class<?> instanceClass = instance.getClass();
		Method[] methods = instanceClass.getMethods();
		for (Method method : methods) {
			Class<?>[] argTypes = method.getParameterTypes();
			StringBuilder methodKeyBuilder = new StringBuilder();
			methodKeyBuilder.append(instanceName).append("#");
			methodKeyBuilder.append(method.getName()).append("$");
			for (Class<?> argClass : argTypes) {
				methodKeyBuilder.append(argClass.getName()).append("_");
			}
			cacheMethods.put(methodKeyBuilder.toString(), method);
		}
	}

	public void handleRequest(final HandshakeRequest request,
			Decoder in, BinaryEncoder out) throws Exception {

		String targetInstanceName = request.getTargetInstanceName();
		String methodName = request.getMethodName();

		Method method = null;
		Exception error = null;
		Object response = null;
		ProcessData processData = processors.get(targetInstanceName);
		if (processData == null) {
			throw new Exception("no " + targetInstanceName
					+ " instance exists on the server");
		}

		Object processor = processData.getInstance();
		Class<?> iface = processData.getIface();
		data = new ReflectData(iface.getClassLoader());
		Protocol protocol = ArvoUtils.getProtocol(iface);
//		Protocol protocol = ((ReflectData)data).getProtocol(iface);
		Message message = protocol.getMessages().get(methodName);

		Schema schema = message.getRequest();// 根据方法名取得请求参数类型

		Object object = getDatumReader(schema, data).read(null, in);

		int numParams = schema.getFields().size();
		Object[] params = new Object[numParams];
		Class<?>[] paramTypes = new Class[numParams];
		String[] argTypes = new String[numParams];
		int i = 0;
		for (Schema.Field param : schema.getFields()) {
			params[i] = ((GenericRecord) object).get(param.name());
			paramTypes[i] = ((SpecificData) data).getClass(param.schema());
			argTypes[i] = paramTypes[i].getName();
			i++;
		}
		if (argTypes != null && argTypes.length > 0) {
			StringBuilder methodKeyBuilder = new StringBuilder();
			methodKeyBuilder.append(targetInstanceName).append("#");
			methodKeyBuilder.append(methodName).append("$");
			for (int j = 0; j < argTypes.length; j++) {
				methodKeyBuilder.append(argTypes[j]).append("_");
			}
			method = cacheMethods.get(methodKeyBuilder.toString());

			if (method == null) {
				throw new Exception("no method: " + methodKeyBuilder.toString()
						+ " find in " + targetInstanceName + " on the server");
			}
		} else {
			method = processor.getClass().getMethod(methodName,
					new Class<?>[] {});
			if (method == null) {
				throw new Exception("no method: " + methodName + " find in "
						+ targetInstanceName + " on the server");
			}
		}
		try {
			response = method.invoke(processor, params);
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof Exception) {
				error = (Exception) e.getTargetException();
			} else {
				error = new Exception(e.getTargetException());
			}
			LOGGER.error("error for target instance:" + targetInstanceName + " invoke method:" + methodName, error);
		} catch (IllegalAccessException e) {
			error = new AvroRuntimeException(e);
		}

		out.writeBoolean(error != null);
		if (error == null)
			getDatumWriter(message.getResponse(), data).write(response, out);
		else
			try {
				getDatumWriter(message.getErrors(), data).write(error, out);
			} catch (UnresolvedUnionException e) { // unexpected error
				e.printStackTrace();
				throw error;
			}
	}

	private DatumReader<Object> getDatumReader(Schema schema, GenericData data) {
		return new ReflectDatumReader<Object>(schema, schema,
				(ReflectData) data);
	}

	private DatumWriter<Object> getDatumWriter(Schema schema, GenericData data) {
		return new ReflectDatumWriter<Object>(schema, (ReflectData) data);
	}
	
	public GenericData getGenericData(){
		return data;
	}
}
