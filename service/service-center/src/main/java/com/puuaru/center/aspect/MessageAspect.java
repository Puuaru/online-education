package com.puuaru.center.aspect;

import lombok.SneakyThrows;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Description: LoginAspect
 * @Author: puuaru
 * @Date: 2023/3/22
 */
@Aspect
@Component
public class MessageAspect {
    private static final String REDIS_HASH_NAME = "count_stat";
    private static final String LOGIN_COUNTER = "login_num";
    private static final String REGISTER_COUNTER = "register_num";

    private static final int LOGIN_CACHE_THRESHOLD = 128;
    private static final int REGISTER_CACHE_THRESHOLD = 64;

    private final ThreadPoolExecutor threadPoolExecutor;
    private final StreamBridge streamBridge;
    private final RedisTemplate redisTemplate;

    @Autowired
    public MessageAspect(ThreadPoolExecutor threadPoolExecutor, StreamBridge streamBridge, RedisTemplate redisTemplate) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.streamBridge = streamBridge;
        this.redisTemplate = redisTemplate;
    }

    @Pointcut("execution(public * com.puuaru.center.controller.UcenterMemberController.login(..))")
    public void localLogin() {
    }

    @Pointcut("execution(public * com.puuaru.center.controller.GithubController.login(..))")
    public void githubLogin() {
    }

    @Pointcut("execution(public * com.puuaru.center.controller.UcenterMemberController.register(..))")
    public void register() {
    }

    @SneakyThrows
    @Before("localLogin() || githubLogin()")
    public void loginCountHandler() {
        updateCounterWithNotification(LOGIN_COUNTER, LOGIN_CACHE_THRESHOLD);
    }

    @SneakyThrows
    @Before("register()")
    public void registerCountHandler() {
        updateCounterWithNotification(REGISTER_COUNTER, REGISTER_CACHE_THRESHOLD);
    }

    private void updateCounterWithNotification(String counterType, Integer threshold) {
        Integer count = (Integer) redisTemplate.opsForHash().get(REDIS_HASH_NAME, counterType);
        // 读取并更新计数，通过位运算求余
        count = count == null ? 1 : (count + 1) & (threshold - 1);
        // 更新计数器
        redisTemplate.opsForHash().put(REDIS_HASH_NAME, counterType, count);
        HashMap<String, String> params = new HashMap<>();
        params.put("type", counterType);
        params.put("date", LocalDate.now(ZoneId.of("GMT-8")).toString());
        if (count == 0) {
            // 到达阈值，则发送消息通知 Statistics 模块更新数据
            threadPoolExecutor.execute(() -> {
                streamBridge.send("updateStatistics-out-0", MessageBuilder.withPayload(params).build());
            });
        }
    }
}
