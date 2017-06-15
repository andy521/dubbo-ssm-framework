package dubbo.common.core.dao;

import java.util.List;
import java.util.Map;

import dubbo.common.page.PageBean;
import dubbo.common.page.PageParam;

/**
 *  @描述： 数据访问层基础支撑接口
 *  @author lsq
 *  @since 1.0
 */

public interface BaseDao<T> {
	
	/**
	 * 根据实体对象新增记录 
	 */
	long insert(T entity);
	
	/**
	 * 批量保存对象 
	 */
	long insert(List<T> list);
	
	/**
	 * 更新实体对应的记录 
	 */
	int update(List<T> list);
	
	/**
	 * 根据ID查找记录 
	 */
	T getById(long id);
	
	/**
	 * 根据ID删除记录 
	 */
	int deleteById(long id);
	
	/**
	 * 分页查询 
	 */
	public PageBean listPage(PageParam pageParam, Map<String, Object> paramMap);
	
	/**
	 * 根据条件查询 
	 */
	public List<T> listBy(Map<String, Object> paramMap);
	
	/**
	 * 根据条件查询 
	 */
	public T getBy(Map<String, Object> paramMap);
}
