果对基础ssm框架还不会搭建的，可以看看前面的博文。如果有疑惑的可以加我微信：lsqddd。

一，合理对框架进行拆分
1，下面先给出我的拆分方法，再进行分析

      dubbo-common-parent    （Maven父配置）
      dubbo-common                 (公共工程)
      dubbo-common-core         (公共core工程)
      dubbo-common-config       (公共配置工程)
      dubbo-facade-user            (用户服务接口)
      dubbo-service-user            (用户服务实现)
      dubbo-web-boss               (服务消费者)
      dubbo-distributed-tools      (分布式工具)

分析：
(1) 我们可以把依赖管理放在parent里面，然后子模块继承即可。方便后面的统一升级。
(2) 然后一些公共工具类等，可以放在dubbo－common。
(3) 关于一些公共的配置，如：数据库连接配置，redis配置等可以放在dubbo-common-config。
(4) 然后我们可以把user服务拆成dubbo-facade-user（包含实体类和userservice接口）和dubbo-service-user(包含dao层和实现userservice接口)。
(5) 然后dubbo-web-boss通过dubbo调用dubbo-service-user。
(6) 在基础框架上，我还拆分出来一个dubbo-distributed-tools用来存放一些分布式工具，如分布式锁，分布式队列，分布式barrier等。

至此基本拆分完成。详细的可以看代码，在代码里面也注释得很清楚了。特别要注意的是模块之间的依赖关系。这样拆分就是为了降低耦合度，所以拆分时候这个要特别注意。


2，分布式工具实现的思路（重点）
这里着重讲思路，每个问题的答案，到时候会进行更新。代码全在[Github](https://github.com/wacxt/dubbo-ssm-framework)上，就不上代码了，不然篇幅太大。
（1）分布式锁
思路：
 * 在获取分布式锁的时候在locker节点下创建临时顺序节点，释放锁的时候删除该临时节点。客户端调用createNode方法在locker下创建临时顺序节点，
 * 然后调用getChildren(“locker”)来获取locker下面的所有子节点，注意此时不用设置任何Watcher。客户端获取到所有的子节点path之后，如果发现自己在之
 * 前创建的子节点序号最小，那么就认为该客户端获取到了锁。如果发现自己创建的节点并非locker所有子节点中最小的，说明自己还没有获取到锁，
 * 可以给自己小的节点设置Watcher，如果他被删除则提示自己，然后自己可以先完成别的工作。
 
 **问题：这里实现的是独占锁，那么结合代码思考一下共享锁如何实现？**

（2）分布式队列
思路：
* 在分布式环境下，实现Queue需要高一致性来保证，那么我们可以这样来设计。
 * 把一个Node当成一个队列，然后children用来存储内容，
 * 利用ZooKeeper提供的顺序递增的模式（会自动在name后面加入一个递增的数字来插入新元素）。
 * 于是在offer时候我们可以使用create，take时候按照顺序把children第一个delete就可以了
 * ZooKeeper保证了各个server上数据是一致的

**问题：这里实现的是先进先出阻塞队列，那么结合代码思考一下如何实现双向队列还有非阻塞队列？**

（3）分布式barrier
思路：
* Barrier是一种控制和协调多个任务触发次序的机制。
 * 简单来说就是用一个屏障把将要执行的任务拦住，等待所有任务都处于可运行状态才放开屏障。
 * 其实在单机上我们可以利用CyclicBarrier来实现这个机制，但是在分布式环境下。
 * 我们可以利用ZooKeeper可以派上用场，我们可以利用一个Node来作为Barrier的实体。
 * 然后要Barrier的任务通过调用exists检测是否Node存在，当需要打开Barrier时候，删除这个Node。
 * 这样ZooKeeper的watch机制会通知到各个任务可以开始执行 
 
 **问题：双栅栏Double Barrier如何实现？结合代码思考一下？**