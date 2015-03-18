package code.core.npc.rpc.netty.serialize;
/**
 */

import static io.netty.buffer.Unpooled.wrappedBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import code.core.npc.rpc.NettyDataPack;
/**
 * Encode Message
 * 
 */
public class NettyProtocolEncoder extends MessageToByteEncoder<NettyDataPack> {
	
	/**
	 * encode msg to ChannelBuffer
	 * 
	 * @param msg
	 *            NettyDataPack from
	 *            NettyServerAvroHandler/NettyClientAvroHandler in the
	 *            pipeline
	 * @return encoded ChannelBuffer
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, NettyDataPack msg,
			ByteBuf out) throws Exception {
		List<ByteBuffer> origs = msg.getDatas();
		List<ByteBuffer> bbs = new ArrayList<ByteBuffer>(
				origs.size() * 2 + 1);
		bbs.add(getPackHeader(msg)); // prepend a pack header including
											// serial number and list size
		for (ByteBuffer b : origs) {
			bbs.add(getLengthHeader(b)); // for each buffer prepend length
											// field
			bbs.add(b);
		}

		out.writeBytes(wrappedBuffer(bbs.toArray(new ByteBuffer[bbs.size()])));
	}

	private ByteBuffer getPackHeader(NettyDataPack dataPack) {
		ByteBuffer header = ByteBuffer.allocate(8);
		header.putInt(dataPack.getSerial());
		header.putInt(dataPack.getDatas().size());
		header.flip();
		return header;
	}

	private ByteBuffer getLengthHeader(ByteBuffer buf) {
		ByteBuffer header = ByteBuffer.allocate(4);
		header.putInt(buf.limit());
		header.flip();
		return header;
	}

}
