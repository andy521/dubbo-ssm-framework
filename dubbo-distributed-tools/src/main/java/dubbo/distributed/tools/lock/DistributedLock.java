package dubbo.distributed.tools.lock;

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
 * @描述：基于ZooKeeper实现分布式锁
 * @author lsq
 * @since 1.0 
 */

/**
 * 思路解释：
 * 在获取分布式锁的时候在locker节点下创建临时顺序节点，释放锁的时候删除该临时节点。客户端调用createNode方法在locker下创建临时顺序节点，
 * 然后调用getChildren(“locker”)来获取locker下面的所有子节点，注意此时不用设置任何Watcher。客户端获取到所有的子节点path之后，如果发现自己在之
 * 前创建的子节点序号最小，那么就认为该客户端获取到了锁。如果发现自己创建的节点并非locker所有子节点中最小的，说明自己还没有获取到锁，
 * 可以给自己小的节点设置Watcher，如果他被删除则提示自己，然后自己可以先完成别的工作。
 */
public class DistributedLock {
	
	private final Logger logger = LoggerFactory.getLogger(DistributedLock.class);
	private final int ZK_SESSION_TIMEOUT = 5000;
	private String root = "/lock-";
	private CountDownLatch countDownLatch = new CountDownLatch(1);
	private ZooKeeper zooKeeper;
	private String myPath;
	
	public DistributedLock(String address, String lockName){
		if(StringUtils.isBlank(address)){
			throw new RuntimeException("zookeeper address can not be empty");
		}
		if(StringUtils.isBlank(lockName)){
			throw new RuntimeException("lockName can not be empty");
		}
		//创建Zookeeper实例
		zooKeeper = connectServer(address);
		if(zooKeeper != null){
			root += lockName;
			try {
                Stat stat = zooKeeper.exists(root, false);
                if (stat == null) {
                    zooKeeper.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
			
		}
	}
	
	/**
	 * 获取锁 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public void lock() throws KeeperException, InterruptedException{
		System.out.println(Thread.currentThread().getName() + " 开始等待锁");
		myPath = zooKeeper.create(root+"/", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
		judgeLock();
	}
	
	/**
	 * 判断是否能够获取锁 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	private void judgeLock() throws KeeperException, InterruptedException{
		List<String> list = zooKeeper.getChildren(root, false);
		System.out.println("此时已有"+list.size()+"在等待");
		String[] nodes = list.toArray(new String[list.size()]);
		Arrays.sort(nodes);
		if(nodes.length > 0){
			if(!myPath.equals(root + "/" + nodes[0])){
				System.out.println(Thread.currentThread().getName() + "正在排队等待锁");
				waitForLock(nodes[0]);
			} else{
				countDownLatch.countDown();
			}
		} else{
			countDownLatch.countDown();
		}
	}
	
	/**
	 * 等待锁(一直在自旋等待锁) 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	private void waitForLock(String nodePath) throws InterruptedException, KeeperException {
        final CountDownLatch latch=new CountDownLatch(1);
        Stat stat = zooKeeper.exists(root + "/" + nodePath, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                latch.countDown();
            }
        });
        if (stat == null) {
            judgeLock();
        } else {
            latch.await();
            judgeLock();
        }
   }
	
	/**
	 * 释放锁 
	 */
	public void unlock() {
        if (StringUtils.isEmpty(myPath)) {
            logger.error("no need to unlock!");
        }
        logger.info(Thread.currentThread().getName() + " 释放锁");
        try {
            zooKeeper.delete(myPath, -1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * 尝试获得锁 
	 * @throws InterruptedException 
	 * @throws KeeperException 
	 */
	public boolean tryLock() throws KeeperException, InterruptedException{
		myPath = zooKeeper.create(root+"/lock_", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		List<String> list = zooKeeper.getChildren(root, false);
		String nodes[] = list.toArray(new String[list.size()]);
		Arrays.sort(nodes);
		if(myPath.equals(root + "/" +nodes[0])){
			return true;
		}else {
			return false;
		}
	}
	
	/**
	 * 连接zookeeper服务器 
	 */
	private ZooKeeper connectServer(String address){
		//创建成功后减一
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
			
			//进行等待
			latch.await();
		} catch (IOException e) {
            logger.error("IOException", e);
        } catch (InterruptedException ex) {
            logger.error("InterruptedException", ex);
        }
		return zk;
	}
	//测试方法
	/*
	public static void main(String args[]){
		DistributedLock lock = new DistributedLock("127.0.0.1:4400","test");
		try {
			
			lock.lock();
			TimeUnit.SECONDS.sleep(10);
			System.out.println(Thread.currentThread().getName()+"开始释放锁");
		} catch (KeeperException e) {						
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally{
			lock.unlock();
		}
		
	}
	*/
}
