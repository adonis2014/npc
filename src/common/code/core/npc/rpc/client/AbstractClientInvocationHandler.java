package code.core.npc.rpc.client;
/**
 *   
 */
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Random;
/**
 * 
 */
public abstract class AbstractClientInvocationHandler implements InvocationHandler {

	private List<InetSocketAddress> servers;
	
	private int clientNums;
	
	private int connectTimeout;
	
	private String targetInstanceName;
	
	private int codecType;
	
	private int protocolType;
	
	private Class<?> iface;
	
	private int methodTimeout;
	
	public AbstractClientInvocationHandler(List<InetSocketAddress> servers,int clientNums,int connectTimeout,
										   String targetInstanceName, int methodTimeout,
										   int codecType,int protocolType, Class<?> iface){
		this.servers = Collections.unmodifiableList(servers);
		this.clientNums = clientNums;
		this.connectTimeout = connectTimeout;
		this.methodTimeout = methodTimeout;
		this.targetInstanceName = targetInstanceName;
		this.codecType = codecType;
		this.protocolType = protocolType;
		this.iface = iface;
	}
	
	public void updateServers(List<InetSocketAddress> servers){
		this.servers = Collections.unmodifiableList(servers);
	}
	
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		InetSocketAddress server = null;
		if(servers.size() == 1){
			server = servers.get(0);
		}
		else{
			// random is not thread-safe,so...
			Random random = new Random();
			server = servers.get(random.nextInt(servers.size()));
		}
		Client client = getClientFactory().get(server.getAddress().getHostAddress(), server.getPort(), connectTimeout, clientNums);
		String methodName = method.getName();
		String[] argTypes = createParamSignature(method.getParameterTypes());
		return client.invokeSync(targetInstanceName, method, methodName, argTypes, args, methodTimeout, codecType, protocolType, iface);
	}
	
	private String[] createParamSignature(Class<?>[] argTypes) {
        if (argTypes == null || argTypes.length == 0) {
            return new String[] {};
        }
        String[] paramSig = new String[argTypes.length];
        for (int x = 0; x < argTypes.length; x++) {
            paramSig[x] = argTypes[x].getName();
        }
        return paramSig;
    }
	
	public abstract ClientFactory getClientFactory();

}
