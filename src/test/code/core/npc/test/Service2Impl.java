package code.core.npc.test;

import org.springframework.stereotype.Service;

@Service("service2")
public class Service2Impl implements Service1 {

	@Override
	public String getTest(String str) {
		return "service 2 " + str;
	}

	@Override
	public void getVoid(String str) throws Exception {
		System.out.println("void service 2 " + str);
	}

	@Override
	public String getNoArg() {
		return "service 2 no arg";
	}

}
