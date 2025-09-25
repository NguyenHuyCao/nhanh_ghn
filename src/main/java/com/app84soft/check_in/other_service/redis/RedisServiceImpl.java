package com.app84soft.check_in.other_service.redis;

import com.app84soft.check_in.services.BaseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class RedisServiceImpl extends BaseService implements RedisService {

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void addCache(String key, Object value) {
        addCache(key, value, 5);
    }

    @Override
    public void addCache(String key, Object value, long timeout) {
        addCache(key, value, timeout, TimeUnit.MINUTES);
    }

    @Override
    public void addCache(String key, Object value, long timeout, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
        } catch (Exception e) {
            log.error("addCache", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCache(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("getCache", e);
            return null;
        }
    }

    @Override
    public void flushCache(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void flushAllCache() {
    }
}
