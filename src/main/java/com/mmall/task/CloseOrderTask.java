package com.mmall.task;


import com.mmall.common.Const;
import com.mmall.common.RedisShardedPool;
import com.mmall.service.IOderService;
import com.mmall.util.PropertiesUtil;
import com.mmall.util.RedisShardedPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
@Slf4j
public class CloseOrderTask {

    @Autowired
    private IOderService iOderService;

    // 使用shutdown关闭tomcat执行此方法;kill不能
    @PreDestroy
    public void delLock(){
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
    }

    @Scheduled(cron = "0 */1 * * * ?") // 每1分钟 （每个1分钟的整数倍）
    public void closeOrderTaskV1() {
        log.info("关闭订单定时任务启动");
        int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
        iOderService.closeOrder(hour);
        log.info("关闭订单定时任务结束");
    }

    @Scheduled(cron = "0 */1 * * * ?") // 每1分钟 （每个1分钟的整数倍）
    public void closeOrderTaskV2() {
        log.info("关闭订单定时任务启动");
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout"));

        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));

        if (setnxResult != null && setnxResult.intValue() == 1){
            //1,代表设置成功,获取锁
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);

        }else{
            log.info("没有获得分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }

        log.info("关闭订单定时任务结束");
    }


    @Scheduled(cron = "0 */1 * * * ?") // 每1分钟 （每个1分钟的整数倍）
    public void closeOrderTaskV3() {
        log.info("关闭订单定时任务启动");
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout"));

        Long setnxResult = RedisShardedPoolUtil.setnx(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));

        if (setnxResult != null && setnxResult.intValue() == 1){
            //1,代表设置成功,获取锁
            closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        }else{
            // 未获取到锁，继续判断，判断时间戳看是否可以重置并获取到锁
            String lockValueStr = RedisShardedPoolUtil.get(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
            if (lockValueStr!=null && System.currentTimeMillis()>Long.parseLong(lockValueStr)){
                // 时间重置，并返回旧值
                String getSetResult =  RedisShardedPoolUtil.getSet(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
                // 再次使用当前时间戳 getSet
                // 返回的给定的key的旧值-》旧值判断，是否可以获取锁
                // 当key没有旧值时，即key不存在时，返回nil -》获取锁
                // set一个新的value,获取旧的值
                if (getSetResult == null || (getSetResult !=null && StringUtils.equals(lockValueStr,getSetResult))){
                    // 真正获取到锁
                    closeOrder(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }else{
                    log.info("没有获取到分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
                }
            }else{
                log.info("没有获取到分布式锁:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);

            }
        }

        log.info("关闭订单定时任务结束");
    }


    private void closeOrder(String  lockName){
        RedisShardedPoolUtil.expire(lockName,50); // 50miao,防止死锁
        log.info("获取{},ThreadName:{}",Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread());
        int hour =  Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour", "2"));
        iOderService.closeOrder(hour);
        RedisShardedPoolUtil.del(Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK);
        log.info("释放{}", Const.REDIS_LOCK.CLOSE_ORDER_TASK_LOCK,Thread.currentThread());
        log.info("==================================");
    }



}
