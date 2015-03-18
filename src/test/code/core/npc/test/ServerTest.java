package code.core.npc.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import code.core.npc.rpc.NamedThreadFactory;
import code.core.npc.rpc.netty.server.NettyServer;
import code.core.npc.rpc.protocol.RPCProtocol;
import code.core.npc.rpc.server.Server;

public class ServerTest {

	public static void main(String[] args) throws Exception {

		Server server = new NettyServer();
		server.registerProcessor(RPCProtocol.TYPE, "helloWorld",
				new Service1Impl(), Service1.class);
		server.registerProcessor(RPCProtocol.TYPE, "helloWorld2",
				new Service2Impl(), Service1.class);
		ThreadFactory tf = new NamedThreadFactory("BUSINESSTHREADPOOL");
		ExecutorService threadPool = new ThreadPoolExecutor(20, 1000, 300,
				TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), tf);
		server.start(8080, threadPool);
		
		server.stop();
	}

}
