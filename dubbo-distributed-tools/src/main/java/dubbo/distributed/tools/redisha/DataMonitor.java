package dubbo.distributed.tools.redisha;

import org.apache.zookeeper.ZooKeeper;

import java.util.Arrays;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * @描述:数据监测器
 * @author lsq
 * @since 1.0 
 */
public class DataMonitor implements Watcher,StatCallback {
	
	String znode;
	boolean dead;
	RedisThread redisThread = new RedisThread();
	DataMonitorListener listener;
	ZooKeeper zk;
	byte prevData[];
	
	public DataMonitor(ZooKeeper zk, String znode, 
            DataMonitorListener listener) {
        this.zk = zk;
        this.znode = znode;
        this.listener = listener;
        zk.exists(znode, true, this, null);
    }

	@SuppressWarnings("deprecation")
	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
		boolean exists;
		switch(rc){
		//接口调用成功
		case Code.Ok:
			exists = true;
			break;
		case Code.NoNode:
			exists = false;
			break;
		case Code.SessionExpired:
		case Code.NoAuth:
			dead = true;
			listener.closing(rc);
			return;
		default:
			zk.exists(znode, true, this, null);
			return;
		}
		
		try{
			//从zookeeper读取IP和port配置
			redisThread.setMasterIP(new String(zk.getData("/redis/master", false, null),"utf-8").split(":")[0]);
			redisThread.setMasterPort(Integer.parseInt(new String(zk.getData("/redis/master", false, null),"utf-8").split(":")[1]));
			redisThread.setSlaveIP(new String(zk.getData("/redis/slave", false, null),"utf-8").split(":")[0]);
			redisThread.setSlavePort(Integer.parseInt(new String(zk.getData("/redis/slave", false, null),"utf-8").split(":")[1]));
			redisThread.setZk(zk);
		}catch(Exception e){
			e.printStackTrace();
		}
		//开启redis监测线程
		new Thread(redisThread).start();
		byte b[] = null;
		if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }
        if ((b == null && b != prevData) || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }
	}

	@SuppressWarnings("deprecation")
	@Override
	public void process(WatchedEvent event) {
		String path = event.getPath();
		System.out.println("process:"+path);
		if(event.getType() == Event.EventType.None){
			switch(event.getState()){
			//正常建立连接
			case SyncConnected:
				break;
			//会话超时
			case Expired:
				dead = true;
				listener.closing(KeeperException.Code.SessionExpired);
				break;
			default:
				break;
			}
		
		} else {
			if(path != null && path.equals(znode)){
				ZooKeeper zk2 = zk;
				zk2.exists(znode, true, this, null);
			}
		}
	}

}
