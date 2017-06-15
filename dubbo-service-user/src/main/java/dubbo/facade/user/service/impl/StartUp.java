package dubbo.facade.user.service.impl;

import org.springframework.context.support.ClassPathXmlApplicationContext;


public class StartUp {
	public static void main(String[] args) {
		try {
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:application.xml");
			context.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		synchronized (StartUp.class) {
			while (true) {
				try {
					StartUp.class.wait();
				} catch (InterruptedException e) {
					System.out.println("error2");
				}
			}
		}
	}
}
