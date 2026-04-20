package com.macro.mall.common.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * 订单号分片算法
 * 根据订单号对10取模进行分表
 * 订单号格式：8位日期 + 2位平台号码 + 2位支付方式 + 6位以上自增id
 * 例如：202604200101000001
 * 
 * 分片策略：使用订单号的最后一位对10取模，或者使用订单号的hashCode对10取模
 * 这里采用订单号的数值对10取模的方式
 */
public class OrderSnShardingAlgorithm implements StandardShardingAlgorithm<String> {

    private Properties props = new Properties();

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<String> shardingValue) {
        String orderSn = shardingValue.getValue();
        if (orderSn == null || orderSn.isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        
        int shardingIndex = calculateShardingIndex(orderSn);
        
        for (String tableName : availableTargetNames) {
            if (tableName.endsWith("_" + shardingIndex)) {
                return tableName;
            }
        }
        
        throw new IllegalArgumentException("未找到对应的分表，订单号：" + orderSn + "，分片索引：" + shardingIndex);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<String> shardingValue) {
        Collection<String> result = new LinkedHashSet<>();
        
        if (shardingValue.getValueRange().hasLowerBound() && shardingValue.getValueRange().hasUpperBound()) {
            String lowerOrderSn = shardingValue.getValueRange().lowerEndpoint();
            String upperOrderSn = shardingValue.getValueRange().upperEndpoint();
            
            int lowerIndex = calculateShardingIndex(lowerOrderSn);
            int upperIndex = calculateShardingIndex(upperOrderSn);
            
            if (lowerIndex == upperIndex) {
                for (String tableName : availableTargetNames) {
                    if (tableName.endsWith("_" + lowerIndex)) {
                        result.add(tableName);
                        break;
                    }
                }
            } else {
                result.addAll(availableTargetNames);
            }
        } else {
            result.addAll(availableTargetNames);
        }
        
        return result;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public String getType() {
        return "ORDER_SN_MOD";
    }

    private int calculateShardingIndex(String orderSn) {
        try {
            if (orderSn.length() >= 1) {
                char lastChar = orderSn.charAt(orderSn.length() - 1);
                if (Character.isDigit(lastChar)) {
                    return Character.getNumericValue(lastChar) % 10;
                }
            }
            return Math.abs(orderSn.hashCode()) % 10;
        } catch (Exception e) {
            return Math.abs(orderSn.hashCode()) % 10;
        }
    }
}
