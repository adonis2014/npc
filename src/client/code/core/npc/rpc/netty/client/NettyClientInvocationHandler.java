package code.core.npc.rpc.netty.client;

/**
 */
import java.net.InetSocketAddress;
import java.util.List;

import code.core.npc.rpc.client.AbstractClientInvocationHandler;
import code.core.npc.rpc.client.ClientFactory;

/**
 * Netty Client Invocation Handler for Client Proxy
 * 
 */
public class NettyClientInvocationHandler extends
		AbstractClientInvocationHandler {

	public NettyClientInvocationHandler(List<InetSocketAddress> servers,
			int clientNums, int connectTimeout, String targetInstanceName,
			int methodTimeout, int codectype, Integer protocolType, Class<?> iface) {
		super(servers, clientNums, connectTimeout, targetInstanceName,
				methodTimeout, codectype, protocolType, iface);
	}

	public ClientFactory getClientFactory() {
		return NettyClientFactory.getInstance();
	}

}
