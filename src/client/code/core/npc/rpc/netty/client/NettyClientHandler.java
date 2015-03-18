package code.core.npc.rpc.netty.client;
/**
 *   
 */
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import code.core.npc.rpc.NettyDataPack;
/**
 * Netty Client Handler
 * 
 */
public class NettyClientHandler extends ChannelHandlerAdapter {
	
	private static final Log LOGGER = LogFactory.getLog(NettyClientHandler.class);
	
	private static final boolean isDebugEnabled = LOGGER.isDebugEnabled();
	
	private NettyClientFactory factory;
	
	private String key;
	
	private NettyClient client;
	
	public NettyClientHandler(NettyClientFactory factory,String key){
		this.factory = factory;
		this.key = key;
	}
	
	public void setClient(NettyClient client){
		this.client = client;
	}
	
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if(msg instanceof List){
			@SuppressWarnings("unchecked")
			List<NettyDataPack> responses = (List<NettyDataPack>)msg;
			if(isDebugEnabled){
				// for performance trace
				LOGGER.debug("receive response list from server: "+ctx.channel().remoteAddress()+",list size is:"+responses.size());
			}
			client.putResponses(responses);
		}
		else if(msg instanceof NettyDataPack){
			NettyDataPack response = (NettyDataPack)msg;
			if(isDebugEnabled){
				// for performance trace
				LOGGER.debug("receive response list from server: "+ctx.channel().remoteAddress()+",request is:" + response.getSerial());
			}
			client.putResponse(response);
		}
		else{
			LOGGER.error("receive message error,only support List || NettyDataPack");
			throw new Exception("receive message error,only support List || NettyDataPack");
		}
	}
	
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
			throws Exception {
		if(!(e.getCause() instanceof IOException)){
			// only log
			LOGGER.error("catch some exception not IOException",e.getCause());
		}
	}
	
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		LOGGER.warn("connection closed: "+ctx.channel().remoteAddress());
		factory.removeClient(key,client);
	}
	
}
