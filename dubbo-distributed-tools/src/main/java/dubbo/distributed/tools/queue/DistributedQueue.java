package dubbo.distributed.tools.queue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @描述：分布式先进先出阻塞队列
 * @author lsq
 * @since 1.0
 */

/**
 * 在分布式环境下，实现Queue需要高一致性来保证，那么我们可以这样来设计。
 * 把一个Node当成一个队列，然后children用来存储内容，
 * 利用ZooKeeper提供的顺序递增的模式（会自动在name后面加入一个递增的数字来插入新元素）。
 * 于是在offer时候我们可以使用create，take时候按照顺序把children第一个delete就可以了
 * 。ZooKeeper保证了各个server上数据是一致的
 * 
 */
public class DistributedQueue {
	
	private final Logger logger = LoggerFactory.getLogger(DistributedQueue.class);
	private final int ZK_SESSION_TIMEOUT = 5000;
	private String root = "/queue-";
	private CountDownLatch countDownLatch;
	private ZooKeeper zooKeeper;
	
	public DistributedQueue(String address,String queueName){
		if (StringUtils.isBlank(address)) {
            throw new RuntimeException("zookeeper address can not be empty!");
        }
        if (StringUtils.isBlank(queueName)) {
            throw new RuntimeException("queueName can not be empty!");
        }
        //连接服务器
        zooKeeper = connectServer(address);
        if(zooKeeper != null){
        	root += queueName;
			try {
				Stat stat = zooKeeper.exists(root, false);
				if(stat == null){
	        		zooKeeper.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
	        	}
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
        	
        }
	}
	
	/**
	 * 往队列存数据 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public boolean put(byte[] data) throws KeeperException, InterruptedException{
		if(data == null || data.length==0)
			throw new RuntimeException("data can not be empty");
		System.out.println(Thread.currentThread().getName()+"is putting data");
		zooKeeper.create(root+"/", data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
		return true;
	}
	
	/**
	 * 取数据 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public byte[] take() throws KeeperException, InterruptedException{
		while(true){
			countDownLatch = new CountDownLatch(1);
			List<String> list = zooKeeper.getChildren(root, new Watcher(){
				@Override
				public void process(WatchedEvent event) {
					countDownLatch.countDown();
				}
			});

			if(list.size()==0){
				countDownLatch.await();
			} else {
				String []nodes = list.toArray(new String[list.size()]);
				Arrays.sort(nodes);
				for (String node : nodes) {
                    try {
                        byte[] data = zooKeeper.getData(root + "/" + node, false, null);
                        zooKeeper.delete(root + "/" + node, -1);
                        return data;
                    } catch (KeeperException.NoNodeException e) {
                    	logger.warn("maybe another thread has deleted it");
                    	continue;
                    }
                }
			}
			
		}
	}
	
	/**
	 * 连接zookeeper服务器 
	 */
	private ZooKeeper connectServer(String address){
		final CountDownLatch latch = new CountDownLatch(1);
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(address, ZK_SESSION_TIMEOUT, new Watcher(){
				@Override
				public void process(WatchedEvent event) {
					if(event.getState() == Event.KeeperState.SyncConnected){
						System.out.println("连接成功");
						latch.countDown();
					}
				}			
			});
			latch.await();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return zk;
	}
	
	public static void main(String args[]){
		DistributedQueue distributedQueue = new DistributedQueue("127.0.0.1:4400","test");
		try {
			distributedQueue.put("string1".getBytes());
			distributedQueue.put("string2".getBytes());
			distributedQueue.put("string3".getBytes());
			distributedQueue.put("string4".getBytes());
			TimeUnit.SECONDS.sleep(5);
			while(true){
				byte[]res = distributedQueue.take();
				System.out.println(new String(res));
			}
		} catch (KeeperException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
