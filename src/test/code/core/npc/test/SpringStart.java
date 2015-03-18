package code.core.npc.test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringStart {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = null;
		try{
			context = new ClassPathXmlApplicationContext(
					new String[] { "spring-config.xml" });
		}catch(Exception e)
		{
			if(context != null)
				context.close();
			e.printStackTrace();
		}

	}
}
