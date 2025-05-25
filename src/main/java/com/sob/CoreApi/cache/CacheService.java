package com.sob.CoreApi.cache;

import java.util.Optional;

public interface CacheService<T> {
    Optional<T> get(String key);
    void put(String key, T value);
    void evict(String key);
}
