package code.core.npc.rpc.netty.client;

/**
 *   
 */
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.ByteBufferOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import code.core.npc.rpc.HandshakeRequest;
import code.core.npc.rpc.NettyDataPack;
import code.core.npc.rpc.client.AbstractClient;
import code.core.npc.rpc.client.Client;
import code.core.npc.rpc.client.ClientFactory;

/**
 * Netty Client
 * 
 */
public class NettyClient extends AbstractClient {

	private static final Log LOGGER = LogFactory.getLog(NettyClient.class);

	private ChannelFuture cf;

	private String key;

	private int connectTimeout;
	
	private static final SpecificDatumWriter<HandshakeRequest> HANDSHAKE_WRITER =
		    new SpecificDatumWriter<HandshakeRequest>(HandshakeRequest.class);
	
	public NettyClient(ChannelFuture cf, String key, int connectTimeout) {
		this.cf = cf;
		this.key = key;
		this.connectTimeout = connectTimeout;
	}

	public void sendRequest(final HandshakeRequest wrapper, Object[] args, final int timeout, final Class<?> iface)
			throws Exception {
		final long beginTime = System.currentTimeMillis();
		final Client self = this;
		
		ByteBufferOutputStream bbo = new ByteBufferOutputStream();  
		Encoder out = EncoderFactory.get().binaryEncoder(bbo, null);
		
		Protocol protocol = getProtocol(iface);
		GenericData data = getGenericData(iface);
		
		Schema schema = protocol.getMessages().get(wrapper.getMethodName()).getRequest();//根据方法名取得请求参数类型
		
		int i = 0;
	    for (Schema.Field param : schema.getFields()){
	    	getDatumWriter(param.schema(), data).write(args[i++], out);
	  	}
		
		out.flush();
		
		List<ByteBuffer> payload = bbo.getBufferList();
		
		HANDSHAKE_WRITER.write(wrapper, out);
		out.flush();
        bbo.append(payload);
        
        NettyDataPack dataPack = new NettyDataPack(wrapper.getId(), bbo.getBufferList());
        
		ChannelFuture lastChannelFuture = cf.channel().writeAndFlush(dataPack);
		// use listener to avoid wait for write & thread context switch
		lastChannelFuture.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future)
					throws Exception {
				if (future.isSuccess()) {
					return;
				}
				String errorMsg = "";
				// write timeout
				if (System.currentTimeMillis() - beginTime >= timeout) {
					errorMsg = "write to send buffer consume too long time("
							+ (System.currentTimeMillis() - beginTime)
							+ "),request id is:" + wrapper.getId();
				}
				if (future.isCancelled()) {
					errorMsg = "Send request to " + cf.channel().toString()
							+ " cancelled by user,request id is:"
							+ wrapper.getId();
				}
				if (!future.isSuccess()) {
					if (cf.channel().isOpen()) {
						// maybe some exception,so close the channel
						cf.channel().close();
					} 
					else {
						NettyClientFactory.getInstance().removeClient(key, self);
					}
					errorMsg = "Send request to " + cf.channel().toString() + " error" + future.cause();
				}
				LOGGER.error(errorMsg);
				/*
				 * comment by liuzh 
				ResponseWrapper response = new ResponseWrapper(wrapper.getId(), 1, wrapper.getProtocolType());
				response.setException(new Exception(errorMsg));
				self.putResponse(response);*/
			}
		});
	}

	private DatumWriter<Object> getDatumWriter(Schema schema, GenericData data) {
	    return new GenericDatumWriter<Object>(schema, data);
	}
	
	public String getServerIP() {
		return ((InetSocketAddress) cf.channel().remoteAddress()).getHostName();
	}

	public int getServerPort() {
		return ((InetSocketAddress) cf.channel().remoteAddress()).getPort();
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public long getSendingBytesSize() {
		// TODO: implement it
		return 0;
	}

	public ClientFactory getClientFactory() {
		return NettyClientFactory.getInstance();
	}

	public void close()
	{
		try {
			cf.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
