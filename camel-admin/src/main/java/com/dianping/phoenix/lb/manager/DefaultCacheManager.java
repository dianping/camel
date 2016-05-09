package com.dianping.phoenix.lb.manager;

import com.dianping.phoenix.lb.api.manager.CacheManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Component;

/**
 * @author mengwenchao
 *         <p/>
 *         2014年12月4日 下午2:27:07
 */
@Component
public class DefaultCacheManager implements CacheManager {

	@Override
	public <K, V> LoadingCache<K, V> createGuavaCache(int count, CacheLoader<K, V> loader) {

		return CacheBuilder.newBuilder().maximumSize(count).build(loader);
	}

}
