package code.core.npc.rpc.netty.server;

/**
 *   
 */
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.ByteBufferInputStream;
import org.apache.avro.util.ByteBufferOutputStream;
import org.apache.avro.util.Utf8;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import code.core.npc.rpc.HandshakeRequest;
import code.core.npc.rpc.HandshakeResponse;
import code.core.npc.rpc.NettyDataPack;
import code.core.npc.rpc.ProtocolFactory;
import code.core.npc.rpc.server.ServerHandler;

/**
 * Netty Server Handler
 * 
 */
public class NettyServerHandler extends ChannelHandlerAdapter {

	private static final Log LOGGER = LogFactory
			.getLog(NettyServerHandler.class);

	private static final SpecificDatumReader<HandshakeRequest> HANDSHAKE_READER = new SpecificDatumReader<HandshakeRequest>(
			HandshakeRequest.class);

	private static final SpecificDatumWriter<HandshakeResponse> HANDSHAKE_WRITER = new SpecificDatumWriter<HandshakeResponse>(
			HandshakeResponse.class);

	private ExecutorService threadpool;

	public NettyServerHandler(ExecutorService threadpool) {
		this.threadpool = threadpool;
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
			throws Exception {
		if (!(e.getCause() instanceof IOException)) {
			// only log
			LOGGER.error("catch some exception not IOException", e.getCause());
		}
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (!(msg instanceof NettyDataPack) && !(msg instanceof List)) {
			LOGGER.error("receive message error,only support NettyDataPack || List");
			throw new Exception(
					"receive message error,only support NettyDataPack || List");
		}
		handleRequest(ctx, msg);
	}

	@SuppressWarnings("unchecked")
	private void handleRequest(final ChannelHandlerContext ctx,
			final Object message) {
		try {
			LOGGER.info(ctx.channel());
			threadpool.execute(new HandlerRunnable(ctx, message, threadpool));
		} catch (RejectedExecutionException exception) {
			LOGGER.error("server threadpool full,threadpool maxsize is:"
					+ ((ThreadPoolExecutor) threadpool).getMaximumPoolSize());
			if (message instanceof List) {
				List<NettyDataPack> requests = (List<NettyDataPack>) message;
				for (final NettyDataPack request : requests) {
					sendErrorResponse(ctx, request);
				}
			} else {
				sendErrorResponse(ctx, (NettyDataPack) message);
			}
		}
	}

	private void sendErrorResponse(final ChannelHandlerContext ctx,
			final NettyDataPack dataPack) {
		List<ByteBuffer> payload = null;
		List<ByteBuffer> handshake = null;
		try {
			Decoder in = DecoderFactory.get().binaryDecoder(
					new ByteBufferInputStream(dataPack.getDatas()), null);

			ByteBufferOutputStream bbo = new ByteBufferOutputStream();
			BinaryEncoder out = EncoderFactory.get().binaryEncoder(bbo, null);

			final HandshakeRequest request = (HandshakeRequest) HANDSHAKE_READER
					.read(null, in);

			HandshakeResponse handshakeResponse = new HandshakeResponse(
					request.getId(), request.getProtocolType());

			HANDSHAKE_WRITER.write(handshakeResponse, out);

			out.flush();
			handshake = bbo.getBufferList();
			bbo = new ByteBufferOutputStream();
			out = EncoderFactory.get().binaryEncoder(bbo, null);
			out.writeBoolean(true);
			writerError(
					Protocol.SYSTEM_ERRORS,
					new ReflectData(Thread.currentThread().getClass()
							.getClassLoader()),
					new Utf8(
							"server threadpool full,maybe because server is slow or too many requests"),
					out);
			if (null == handshake) {
				handshake = new ByteBufferOutputStream().getBufferList();
			}
			out.flush();
			payload = bbo.getBufferList();

			bbo.prepend(handshake);
			bbo.append(payload);
			dataPack.setDatas(bbo.getBufferList());

			ChannelFuture wf = ctx.channel().writeAndFlush(dataPack);
			wf.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future)
						throws Exception {
					if (!future.isSuccess()) {
						LOGGER.error("server write response error,request id is: "
								+ request.getId());
					}
				}
			});

		} catch (IOException e) {
			LOGGER.error("server write response IOException error: "
					+ e.toString());
		}
	}

	class HandlerRunnable implements Runnable {

		private ChannelHandlerContext ctx;

		private Object message;

		private ExecutorService threadPool;

		private ServerHandler serverHandler;

		public HandlerRunnable(ChannelHandlerContext ctx, Object message,
				ExecutorService threadPool) {
			this.ctx = ctx;
			this.message = message;
			this.threadPool = threadPool;
		}

		@SuppressWarnings("rawtypes")
		public void run() {
			// pipeline
			try {
				if (message instanceof List) {
					List messages = (List) message;
					for (Object messageObject : messages) {
						threadPool.execute(new HandlerRunnable(ctx,
								messageObject, threadPool));
					}
				} else {
					NettyDataPack dataPack = (NettyDataPack) message;

					Decoder in = DecoderFactory.get().binaryDecoder(
							new ByteBufferInputStream(dataPack.getDatas()),
							null);

					HandshakeRequest request = null;
					ByteBufferOutputStream bbo = new ByteBufferOutputStream();
					BinaryEncoder out = EncoderFactory.get().binaryEncoder(bbo,
							null);
					long beginTime = System.currentTimeMillis();
					List<ByteBuffer> payload = null;
					List<ByteBuffer> handshake = null;
					try {
						request = HANDSHAKE_READER.read(null, in);

						HandshakeResponse handshakeResponse = new HandshakeResponse(
								request.getId(), request.getProtocolType());

						HANDSHAKE_WRITER.write(handshakeResponse, out);

						out.flush();
						handshake = bbo.getBufferList();

						beginTime = System.currentTimeMillis();
						serverHandler = ProtocolFactory
								.getServerHandler(request.getProtocolType());
						serverHandler.handleRequest(request, in, out);

					} catch (Exception e) {
						LOGGER.error("server handle request error", e);
						bbo = new ByteBufferOutputStream();
						out = EncoderFactory.get().binaryEncoder(bbo, null);
						out.writeBoolean(true);
						writerError(Protocol.SYSTEM_ERRORS,
								serverHandler.getGenericData(),
								new Utf8(e.toString()), out);
						if (null == handshake) {
							handshake = new ByteBufferOutputStream()
									.getBufferList();
						}
					}

					out.flush();
					payload = bbo.getBufferList();

					bbo.prepend(handshake);
					bbo.append(payload);
					dataPack.setDatas(bbo.getBufferList());

					final int id = request.getId();
					// already timeout,so not return
					if ((System.currentTimeMillis() - beginTime) >= request
							.getTimeout()) {
						LOGGER.warn("timeout,so give up send response to client,requestId is:"
								+ id
								+ ",client is:"
								+ ctx.channel().remoteAddress()
								+ ",consumetime is:"
								+ (System.currentTimeMillis() - beginTime)
								+ ",timeout is:" + request.getTimeout());
						return;
					}
					ChannelFuture wf = ctx.channel().writeAndFlush(dataPack);
					wf.addListener(new ChannelFutureListener() {
						public void operationComplete(ChannelFuture future)
								throws Exception {
							if (!future.isSuccess()) {
								LOGGER.error("server write response error,request id is: "
										+ id);
							}
						}
					});
				}
			} catch (IOException e) {
				LOGGER.error("server handle encoder exception error", e);
			}
		}

	}

	private void writerError(Schema schema, GenericData data, Object error,
			Encoder out) throws IOException {
		if (error instanceof CharSequence)
			error = error.toString();

		new ReflectDatumWriter<Object>(schema, (ReflectData) data).write(error,
				out);
	}

}
