package userdao.test;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import dubbo.facade.user.entity.User;
import dubbo.facade.user.service.UserService;
import dubbo.facade.user.service.impl.StartUp;
import dubbo.service.user.dao.UserDao;

public class UserDaoTest {

	private UserService userService;
	@Test
	public void test(){
		try {
			ApplicationContext context = new ClassPathXmlApplicationContext(
					"classpath:application.xml"); 
			userService = (UserService) context.getBean("userService");
			List<User> res = userService.getUserList(0, 10);
			System.out.println(res.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
