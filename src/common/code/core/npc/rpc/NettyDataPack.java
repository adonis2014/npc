package code.core.npc.rpc;

import java.nio.ByteBuffer;
import java.util.List;

public class NettyDataPack {

	private int serial;
	
	private List<ByteBuffer> datas;
	
	public NettyDataPack(int serial, List<ByteBuffer> datas) {
		this.serial = serial;
		this.datas = datas;
	}

	public int getSerial() {
		return serial;
	}

	public void setSerial(int serial) {
		this.serial = serial;
	}

	public List<ByteBuffer> getDatas() {
		return datas;
	}

	public void setDatas(List<ByteBuffer> datas) {
		this.datas = datas;
	}
	
}
