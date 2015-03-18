package code.core.npc.rpc.netty.client;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;

import code.core.npc.rpc.protocol.RPCProtocol;

public class ClientRequestor {

	@SuppressWarnings("unchecked")
	public static <T> T getClient(List<InetSocketAddress> servers,
			String serviceName, Class<T> iface) {
		return (T) Proxy.newProxyInstance(iface.getClassLoader(),
				new Class<?>[] { iface }, new NettyClientInvocationHandler(
						servers, 1, 50000, serviceName,
						50000, 1, RPCProtocol.TYPE, iface));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getClient(List<InetSocketAddress> servers,
			String serviceName, Class<T> iface, int clientNums,
			int connectTimeout, int methodTimeout) {
		return (T) Proxy.newProxyInstance(iface.getClassLoader(),
				new Class<?>[] { iface }, new NettyClientInvocationHandler(
						servers, clientNums, connectTimeout, serviceName,
						methodTimeout, 1, RPCProtocol.TYPE, iface));
	}
}
