package dubbo.facade.user.service.impl;
 
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dubbo.facade.user.entity.User;
import dubbo.facade.user.service.UserService;
import dubbo.service.user.dao.UserDao;

@Service("userService")
public class UserServiceImpl implements UserService {
	
	@Autowired
	private UserDao userDao;

	@Override
	public List<User> getUserList(int offset, int limit) {
		return userDao.queryAll(offset, limit);
	}

}
