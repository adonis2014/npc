package code.core.npc.rpc.netty.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import code.core.npc.rpc.NamedThreadFactory;
import code.core.npc.rpc.protocol.RPCProtocol;
import code.core.npc.rpc.server.Server;

public class NettyServerStart {
	
	private String[] serviceNames;
	private Object[] services;
	private Class<?>[] ifaces;
	private int corePoolSize;
	private int maximumPoolSize;
	private long keepAliveTime;
	private int listenPort;

	public String[] getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(String[] serviceNames) {
		this.serviceNames = serviceNames;
	}

	public Object[] getServices() {
		return services;
	}

	public void setServices(Object[] services) {
		this.services = services;
	}

	public Class<?>[] getIfaces() {
		return ifaces;
	}

	public void setIfaces(Class<?>[] ifaces) {
		this.ifaces = ifaces;
	}

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}

	public int getMaximumPoolSize() {
		return maximumPoolSize;
	}

	public void setMaximumPoolSize(int maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
	}

	public long getKeepAliveTime() {
		return keepAliveTime;
	}

	public void setKeepAliveTime(long keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}

	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	public void start() throws Exception {
		Server server = new NettyServer();
		for (int i = 0; i < serviceNames.length; i++) {
			server.registerProcessor(RPCProtocol.TYPE, serviceNames[i],
					services[i], ifaces[i]);
		}

		ThreadFactory tf = new NamedThreadFactory("BUSINESSTHREADPOOL");
		ExecutorService threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime,
				TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), tf);
		server.start(listenPort, threadPool);
	}
}
