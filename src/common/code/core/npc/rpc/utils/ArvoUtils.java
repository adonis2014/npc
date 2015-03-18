package code.core.npc.rpc.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;

public class ArvoUtils {

	public static Protocol parse(String fileName) {
		try {
			URL url = Thread.currentThread().getClass().getResource(fileName);
			
			if(url == null)
				url = Thread.currentThread().getContextClassLoader().getResource(fileName);
			return Protocol.parse(new File(url.getPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Schema parse2Schema(String fileName, String name) {
		return parse(fileName).getType(name);
	}

	public static Protocol getProtocol(Class<?> iface) {
		try {
			Protocol p = (Protocol) (iface.getDeclaredField("PROTOCOL")
					.get(null));
			if (!p.getNamespace().equals(iface.getPackage().getName()))
				// HACK: protocol mismatches iface. maven shade plugin? try
				// replacing.
				p = Protocol.parse(p.toString().replace(p.getNamespace(),
						iface.getPackage().getName()));
			return p;
		} catch (NoSuchFieldException e) {
			throw new AvroRuntimeException("Not a Specific protocol: " + iface);
		} catch (IllegalAccessException e) {
			throw new AvroRuntimeException(e);
		}
	}
}
