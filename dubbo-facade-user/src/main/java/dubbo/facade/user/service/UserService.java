package dubbo.facade.user.service;

import java.util.List;

import dubbo.facade.user.entity.User;

public interface UserService {
	
	List<User> getUserList(int offset, int limit);
}
