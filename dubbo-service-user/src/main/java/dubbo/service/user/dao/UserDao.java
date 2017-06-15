package dubbo.service.user.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import dubbo.facade.user.entity.User;



public interface UserDao {    
    
    /**
     * 根据偏移量查询用户列表
     */
    List<User> queryAll(@Param("offset") int offset, @Param("limit") int limit);
	
}
