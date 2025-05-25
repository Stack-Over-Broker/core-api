package com.sob.CoreApi.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class RedisCacheService<T> implements CacheService<T> {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DataProvider<T> dataProvider;
    private final Class<T> typeClass;
    private final Duration ttl;
    private final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, DataProvider<T> dataProvider, Class<T> typeClass, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.dataProvider = dataProvider;
        this.typeClass = typeClass;
        this.ttl = ttl;
    }

    @Override
    public Optional<T> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.info("[CACHE HIT] Key {}", key);
                return Optional.of(objectMapper.readValue(json, typeClass));
            }

            log.info("[CACHE MISS] Key {}, buscando no banco...", key);
            T fallbackData = dataProvider.load(key);
            put(key, fallbackData);
            return Optional.ofNullable(fallbackData);
        } catch (JsonProcessingException e) {
            log.error("Erro ao desserializar cache para chave {}", key, e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Erro ao buscar key {} do cache", key, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
            log.info("[CACHE PUT] Key {} adicionada com TTL {}", key, ttl);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar objeto para cache key {}", key, e);
        }
    }

    @Override
    public void evict(String key) {
        redisTemplate.delete(key);
        log.info("[CACHE EVICT] Key {} removida", key);
    }
}
