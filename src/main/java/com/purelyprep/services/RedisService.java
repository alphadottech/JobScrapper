package com.purelyprep.services;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RedisService {

    private static RedisService redisService;

    public static RedisService getInstance() {
        return redisService;
    }

    @PostConstruct
    public void init() {
        redisService = this;
    }

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisService(@Qualifier("stringToObjectRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static String getSetKey(Class clazz) {
        return clazz.getSimpleName() + "-set";
    }

    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void delete(String key) {
        redisTemplate.unlink(key);
    }

    public <T> void addToSet(String setKey, T setValue) {
        redisTemplate.opsForSet().add(setKey, setValue);
    }

    public <T> void removeFromSet(String setKey, T setValue) {
        redisTemplate.opsForSet().remove(setKey, setValue);
    }

    public <T> Set<T> members(String setKey) {
        return (Set<T>) redisTemplate.opsForSet().members(setKey);
    }
}
