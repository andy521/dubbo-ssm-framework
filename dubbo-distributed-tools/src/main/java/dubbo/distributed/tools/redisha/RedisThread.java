package dubbo.distributed.tools.redisha;

import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.ZooKeeper;

import redis.clients.jedis.Jedis;

/**
 * @描述：Redis监控线程，暂时定为一主一从，后续再扩展
 * @author lsq
 * @since 1.0 
 */
public class RedisThread implements Runnable{
	
	private final String rootPath = "/redis";
	private String masterIP;
	private int masterPort;
	private String slaveIP;
	private int slavePort;
	private String path;
	private ZooKeeper zk;
	
	@Override
	public void run() {
		Jedis masterJedis = null;
		Jedis slaveJedis = new Jedis(slaveIP,slavePort);
		try{
			while(true){
				String currdata = null;
				masterJedis = new Jedis(masterIP, masterPort);
				currdata = masterJedis.ping();
				String data = new String(zk.getData(rootPath, false, null),"utf-8");
				if(!"PONG".equals(currdata) && data.equals("master")){
					System.out.println("主服务故障，从服务顶上，IP为:"+slaveIP+",端口为:"+slavePort);
					slaveJedis.slaveofNoOne();
					//设置提示当前是从服务器
					zk.setData(rootPath, "slave".getBytes(), -1);
					//设置master节点信息
					if(path.equals("/redis/master")){
						zk.setData(path, (masterIP+":"+masterPort+":"+"not working").getBytes(), -1);
					}
					
				}
				if("PONG".equals(currdata) && data.equals("slave")){
					System.out.println("主服务器从故障中恢复");
					//先follow从服务器恢复数据
					masterJedis.slaveof(slaveIP, slavePort);
					//休眠五秒等待数据同步完成
					TimeUnit.SECONDS.sleep(5);
					System.out.println("主服务器取代从服务器继续服务");
					masterJedis.slaveofNoOne();
					slaveJedis.slaveof(masterIP, masterPort);
					zk.setData(rootPath, "master".getBytes(), -1);
					if(path.equals("/zk/master")){
						zk.setData(path, (masterIP+":"+masterPort+":"+"avaliable").getBytes(), -1);
					}
				}
				//每隔三秒监测一次
				TimeUnit.SECONDS.sleep(3);
			}
			
		}catch (Exception e) {
    		e.printStackTrace();
		}
		
	}

	public String getMasterIP() {
		return masterIP;
	}

	public void setMasterIP(String masterIP) {
		this.masterIP = masterIP;
	}

	public int getMasterPort() {
		return masterPort;
	}

	public void setMasterPort(int masterPort) {
		this.masterPort = masterPort;
	}

	public String getSlaveIP() {
		return slaveIP;
	}

	public void setSlaveIP(String slaveIP) {
		this.slaveIP = slaveIP;
	}

	public int getSlavePort() {
		return slavePort;
	}

	public void setSlavePort(int slavePort) {
		this.slavePort = slavePort;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ZooKeeper getZk() {
		return zk;
	}

	public void setZk(ZooKeeper zk) {
		this.zk = zk;
	}

	public String getRootPath() {
		return rootPath;
	}
	
}
