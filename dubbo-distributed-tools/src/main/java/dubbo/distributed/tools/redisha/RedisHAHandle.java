package dubbo.distributed.tools.redisha;

import java.io.IOException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class RedisHAHandle implements Watcher, Runnable, DataMonitorListener{
	
	private String rootPath = "/redis";
	private String znode;
	private DataMonitor dm;
	private ZooKeeper zk;
	private int SESSION_TIME_OUT = 5000;
	
	public RedisHAHandle(){
		super();
	}
	
	public RedisHAHandle(String hostPort,String IP,int Port, String type, String filename
            ) throws KeeperException, IOException, InterruptedException {
		zk = new ZooKeeper(hostPort, SESSION_TIME_OUT, this);
		if(zk.exists("/redis", false) == null){
        	zk.create("/redis", "redis".getBytes(), Ids.OPEN_ACL_UNSAFE,CreateMode.PERSISTENT);
        }
        String znode = null;
        if("master".equals(type.toLowerCase())){
        	if(zk.exists("/redis/master", false) == null){
        		znode = zk.create("/redis/master", (IP+":"+ Port).getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        	}
        	zk.setData(rootPath, "master".getBytes(), -1);
        	zk.setData("/redis/master", (IP+":"+ Port).getBytes(), -1);
        	znode = "/redis/master";
        }else{
        	if(zk.exists("/redis/slave", false) == null){
        		znode = zk.create("/redis/slave", (IP+":"+ Port).getBytes(), Ids.OPEN_ACL_UNSAFE,CreateMode. PERSISTENT);
        	}
        	zk.setData("/zk/slave", (IP+":"+ Port).getBytes(), -1);
        	znode = "/zk/slave";
        }
        dm = new DataMonitor(zk, znode, this);
	}
	
	@Override
	public void exists(byte[] data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closing(int rc) {
		synchronized (this) {
            notify();
        }	
	}

	@Override
	public void run() {
		try {
            synchronized (this) {
                while (!dm.dead) {
                  wait();
                }
            }
        } catch (InterruptedException e) {
        }	
	}

	@Override
	public void process(WatchedEvent event) {
		dm.process(event);	
	}

}
