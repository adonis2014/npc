package code.core.npc.rpc.netty.server;
/**
 *   
 */
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogLevel;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import code.core.npc.rpc.NamedThreadFactory;
import code.core.npc.rpc.ProtocolFactory;
import code.core.npc.rpc.netty.serialize.NettyProtocolDecoder;
import code.core.npc.rpc.netty.serialize.NettyProtocolEncoder;
import code.core.npc.rpc.server.Server;

/**
 * Netty Server
 * 
 */
public class NettyServer implements Server {

	private static final Log LOGGER = LogFactory.getLog(NettyServer.class);
	
	private ServerBootstrap bootstrap = null;
	
	private EventLoopGroup bossGroup;
	
	private NioEventLoopGroup workerGroup;

	private AtomicBoolean startFlag = new AtomicBoolean(false);
	
	private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
	
	public NettyServer() {
		ThreadFactory serverBossTF = new NamedThreadFactory("NETTYSERVER-BOSS-");
		ThreadFactory serverWorkerTF = new NamedThreadFactory("NETTYSERVER-WORKER-");
		bossGroup = new NioEventLoopGroup(PROCESSORS, serverBossTF);
		workerGroup = new NioEventLoopGroup(PROCESSORS * 2,serverWorkerTF);
		workerGroup.setIoRatio(Integer.parseInt(System.getProperty("npc.rpc.io.ratio", "50")));
		bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup,workerGroup)
			     .channel(NioServerSocketChannel.class)
			     .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			     .option(ChannelOption.SO_REUSEADDR, Boolean.parseBoolean(System.getProperty("npc.rpc.tcp.reuseaddress", "true")))
			     .option(ChannelOption.TCP_NODELAY, Boolean.parseBoolean(System.getProperty("npc.rpc.tcp.nodelay", "true")));
	}

	public void start(int listenPort, final ExecutorService threadPool) throws Exception {
		if(!startFlag.compareAndSet(false, true)){
			return;
		}
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

			protected void initChannel(SocketChannel channel) throws Exception {
				ChannelPipeline pipeline = channel.pipeline();
				pipeline.addLast("logging", new LoggingHandler(LogLevel.INFO));
				pipeline.addLast("decoder", new NettyProtocolDecoder());
				pipeline.addLast("encoder", new NettyProtocolEncoder());
				pipeline.addLast("handler", new NettyServerHandler(threadPool));
			}
			
		});
		ChannelFuture channelFuture = bootstrap.bind(new InetSocketAddress(listenPort)).sync();
		LOGGER.warn("Server started,listen at: "+listenPort);
		
//		channelFuture.channel().closeFuture().sync();
	}

	public void registerProcessor(int protocolType, String serviceName, Object serviceInstance, Class<?> iface) {
		ProtocolFactory.getServerHandler(protocolType).registerProcessor(serviceName, serviceInstance, iface);
	}
	
	public void stop() throws Exception {
		LOGGER.warn("Server stop!");
		startFlag.set(false);
		bossGroup.shutdownGracefully();  
        workerGroup.shutdownGracefully(); 
	}

}
