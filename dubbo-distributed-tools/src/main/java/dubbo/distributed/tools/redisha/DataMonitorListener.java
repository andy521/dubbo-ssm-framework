package dubbo.distributed.tools.redisha;

/**
 * @描述：数据监测接口
 * @author lsq
 * @since 1.0
 *  
 */
public interface DataMonitorListener {
	
	void exists(byte data[]);
	
	void closing(int rc);
}
