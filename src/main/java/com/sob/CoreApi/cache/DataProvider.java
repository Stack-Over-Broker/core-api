package com.sob.CoreApi.cache;

public interface DataProvider<T> {
    T load(String key);
}
