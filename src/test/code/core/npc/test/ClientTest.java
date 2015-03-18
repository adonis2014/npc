package code.core.npc.test;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import code.core.npc.rpc.netty.client.ClientRequestor;
import code.core.npc.rpc.netty.client.NettyClientInvocationHandler;
import code.core.npc.rpc.protocol.RPCProtocol;

public class ClientTest {

	public static void main(String[] args) throws Exception {
		
		for(int i = 0; i < 1; i++){
			new ClientTest().new ClientThread().start();
		}
	}
	
	class ClientThread extends Thread{

		@Override
		public void run() {
			
			Map<String, Integer> methodTimeouts = new HashMap<String, Integer>();
			// so u can specialize some method timeout
			methodTimeouts.put("*", 50000);
			List<InetSocketAddress> servers = new ArrayList<InetSocketAddress>();
			servers.add(new InetSocketAddress("127.0.0.1", 8080));
			// Protocol also support Protobuf & Java,if u use Protobuf,u need call
			// PBDecoder.addMessage first.
			int codectype = 4;
			int clientNums = 2;
			int connectTimeout = 500000000;
			Service1 service = (Service1) ClientRequestor.getClient(servers, "helloWorld2", Service1.class, clientNums,
							connectTimeout, 50000);

			try {
				StringBuffer sb = new StringBuffer();
				
				for(int i =0; i < 1; i++)
				{
					sb.append("Hello,," + "$$$$$$$$$$$$$$$$$$");
				}
				
				System.out.println(service.getTest(sb.toString()));
				System.out.println(service.getNoArg());
				service.getVoid("is it void");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			

		}
		
	}

}
