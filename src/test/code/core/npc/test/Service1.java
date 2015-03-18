package code.core.npc.test;

import code.core.npc.rpc.utils.ArvoUtils;

public interface Service1 {
	public static final org.apache.avro.Protocol PROTOCOL = org.apache.avro.Protocol.parse("{\"protocol\":\"Service1\",\"namespace\":\"code.core.npc.test\",\"types\":[],\"messages\":{\"getTest\":{\"request\":[{\"name\":\"str\",\"type\":\"string\"}],\"response\":\"string\"},\"getVoid\":{\"error\":\"java.lang.Exception\",\"request\":[{\"name\":\"str\",\"type\":\"string\"}],\"response\":\"null\"},\"getNoArg\":{\"request\":[],\"response\":\"string\"}}}");
	String getTest(String str);
	void getVoid(String str) throws Exception;
	String getNoArg();
}
