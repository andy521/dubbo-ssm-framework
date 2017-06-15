package dubbo.distributed.tools.barrier;

import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;


/**
 * @描述：分布式Barrier 
 * @author lsq
 * @since 1.0
 */

/**
 * 思路：Barrier是一种控制和协调多个任务触发次序的机制。
 * 简单来说就是用一个屏障把将要执行的任务拦住，等待所有任务都处于可运行状态才放开屏障。
 * 其实在单机上我们可以利用CyclicBarrier来实现这个机制，但是在分布式环境下。
 * 我们可以利用ZooKeeper可以派上用场，我们可以利用一个Node来作为Barrier的实体。
 * 然后要Barrier的任务通过调用exists检测是否Node存在，当需要打开Barrier时候，删除这个Node。
 * 这样ZooKeeper的watch机制会通知到各个任务可以开始执行 
 */
public class DistributedBarrier implements Watcher {

    private final String addr;
    private ZooKeeper zk = null;
    private final int ZK_SESSION_TIMEOUT = 5000;
    private Integer mutex;
    private int size = 0;
    private String root = "/barrier";

    public DistributedBarrier(String addr, int size) {
    	this.addr = addr;
        this.size = size;

        try {
            zk = new ZooKeeper(addr, ZK_SESSION_TIMEOUT, this);
            mutex = new Integer(-1);
            Stat s = zk.exists(root, false);
            if (s == null) {
                zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 当触发事件后，唤醒在mutex上等待的线程
     * 只要是zk服务器上节点的数据发生改变（不管哪个zk client改变了数据），
     * 这里都会接收到相应的事件，从而唤醒相应的线程，做出相应的处理
     *
     */
    public synchronized void process(WatchedEvent event) {
        synchronized (mutex) {
            mutex.notify();
        }
    }

    /**
     * 当新建znode时，首先持有mutex监视器才能进入同步代码块。
     * 当znode发生事件后，会触发process，从而唤醒在mutex上等待的线程。
     * 通过while循环判断创建的节点个数，当节点个数大于设定的值时，这个enter方法才执行完成。
     * @throws Exception
     */
    public boolean enter(String name) throws Exception {
        zk.create(root + "/" + name, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() < size) {
                    mutex.wait();
                } else {
                    return true;
                }
            }
        }
    }

    /**
     * 同理。对于leave方法，当delete znode时，触发事件，从而唤醒mutex上等待的线程，通过while循环
     * 判断节点的个数，当节点全部删除后，leave方法结束。
     * 从而使整个添加删除znode的线程结束
     * @throws KeeperException
     * @throws InterruptedException
     */
    public boolean leave(String name) throws KeeperException, InterruptedException {
        zk.delete(root + "/" + name, 0);
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                if (list.size() > 0) {
                    mutex.wait();
                } else {
                    return true;
                }
            }
        }
    }
    
    //测试主类
    /*
    public static void main(String args[]) throws Exception {
        for (int i = 0; i < 3; i++) {
            Process p = new Process("Thread-" + i, new DistributedBarrier("127.0.0.1:4400", 3));
            p.start();
        }
    }
    */
}

class Process extends Thread {

    private String name;
    private DistributedBarrier barrier;

    public Process(String name, DistributedBarrier barrier) {
        this.name = name;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        try {
            barrier.enter(name);
            System.out.println(name + " enter");
            Thread.sleep(1000 + new Random().nextInt(2000));
            barrier.leave(name);
            System.out.println(name + " leave");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}