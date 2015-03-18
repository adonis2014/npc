package code.core.npc.rpc.server;

public class ProcessData {
	private Object instance;
	private Class<?> iface;

	public ProcessData(Object instance, Class<?> iface) {
		super();
		this.instance = instance;
		this.iface = iface;
	}

	public Object getInstance() {
		return instance;
	}

	public void setInstance(Object instance) {
		this.instance = instance;
	}

	public Class<?> getIface() {
		return iface;
	}

	public void setIface(Class<?> iface) {
		this.iface = iface;
	}
}
