package com.macro.mall.common.sharding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 订单ID与订单号映射服务
 * 用于分表后通过orderId查询时，先获取orderSn再路由到正确的分片
 */
@Service
public class OrderIdSnMappingService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.database:0}")
    private String redisDatabase;

    private static final String ORDER_ID_SN_KEY_PREFIX = "order:id:sn:";
    private static final long EXPIRE_DAYS = 30;

    /**
     * 存储orderId到orderSn的映射
     * @param orderId 订单ID
     * @param orderSn 订单号
     */
    public void saveMapping(Long orderId, String orderSn) {
        String key = buildKey(orderId);
        redisTemplate.opsForValue().set(key, orderSn, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 根据orderId获取orderSn
     * @param orderId 订单ID
     * @return 订单号，如果不存在返回null
     */
    public String getOrderSn(Long orderId) {
        String key = buildKey(orderId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除映射
     * @param orderId 订单ID
     */
    public void removeMapping(Long orderId) {
        String key = buildKey(orderId);
        redisTemplate.delete(key);
    }

    private String buildKey(Long orderId) {
        return redisDatabase + ":" + ORDER_ID_SN_KEY_PREFIX + orderId;
    }
}
