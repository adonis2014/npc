package code.core.npc.rpc.netty.serialize;
/**
 *   
 */
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.AvroRuntimeException;

import code.core.npc.rpc.NettyDataPack;
/**
 * decode byte[]
 * 
 */
public class NettyProtocolDecoder extends ByteToMessageDecoder {
	
	private boolean packHeaderRead = false;
	private int listSize;
	private NettyDataPack dataPack;
	private final long maxMem;
	private static final long SIZEOF_REF = 8L; // mem usage of 64-bit
												// pointer

	public NettyProtocolDecoder() {
		maxMem = Runtime.getRuntime().maxMemory();
	}

	/**
	 * decode buffer to NettyDataPack
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg,
			List<Object> out) throws Exception {

		if (!packHeaderRead) {
			if (decodePackHeader(ctx, msg, out)) {
				packHeaderRead = true;
			}
		}else{
			if (decodePackBody(ctx, msg, out)) {
				packHeaderRead = false; // reset state
				out.add(dataPack);
			}
		}
	}

	private boolean decodePackHeader(ChannelHandlerContext ctx,
			ByteBuf msg, List<Object> out) throws Exception {
		if (msg.readableBytes() < 8) {
			return false;
		}

		int serial = msg.readInt();
		int listSize = msg.readInt();
		
		// Sanity check to reduce likelihood of invalid requests being
		// honored.
		// Only allow 10% of available memory to go towards this list (too
		// much!)
		if (listSize * SIZEOF_REF > 0.1 * maxMem) {
			ctx.channel().close().await();
			throw new AvroRuntimeException(
					"Excessively large list allocation "
							+ "request detected: " + listSize
							+ " items! Connection closed.");
		}

		this.listSize = listSize;
		dataPack = new NettyDataPack(serial, new ArrayList<ByteBuffer>(
				listSize));
		
		return true;
	}

	private boolean decodePackBody(ChannelHandlerContext ctx, ByteBuf msg,
			List<Object> out) throws Exception {

		if (msg.readableBytes() < 4) {
			return false;
		}

		msg.markReaderIndex();

		int length = msg.readInt();

		if (msg.readableBytes() < length) {
			msg.resetReaderIndex();
			return false;
		}

		ByteBuffer bb = ByteBuffer.allocate(length);
		msg.readBytes(bb);
		bb.flip();
		dataPack.getDatas().add(bb);

		return dataPack.getDatas().size() == listSize;
	}

}
