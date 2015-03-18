package code.core.npc.test;

import org.springframework.stereotype.Service;

@Service("service1")
public class Service1Impl implements Service1 {

	@Override
	public String getTest(String str) {
		return "hello " + str;
	}

	@Override
	public void getVoid(String str) throws Exception {
		System.out.println("the result is :" + str);
		if(true)
			throw new Exception("it is exception test");
	}

	@Override
	public String getNoArg() {
		return "it is no arg";
	}

}
