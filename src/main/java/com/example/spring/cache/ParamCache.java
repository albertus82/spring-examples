package com.example.spring.cache;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ParamCache {

	@Autowired
	private ParamRepository repository;

	@Value("${cache.param.maxEntries:100}") // TODO choose an appropriate default value
	private int maxEntries;

	private final Map<String, Map<ParamType, Long>> cache = Collections.synchronizedMap(new LinkedHashMap<String, Map<ParamType, Long>>(16, 0.75f, true) {

		private static final long serialVersionUID = 1912349338952347349L;

		@Override
		protected boolean removeEldestEntry(final Entry<String, Map<ParamType, Long>> eldest) {
			if (size() > maxEntries) {
				log.debug("Removing entry: {}", eldest);
				return true;
			}
			else {
				return false;
			}
		}
	});

	private LocalDate lastClearedDate = LocalDate.now();

	public Map<ParamType, Long> get(final String key) {
		final LocalDate currentDate = LocalDate.now();
		if (currentDate.isAfter(lastClearedDate)) {
			clear(currentDate);
		}
		Map<ParamType, Long> value = cache.get(key);
		if (value != null) {
			log.debug("Cache hit for key: {}", key);
			return value;
		}
		else {
			log.debug("Cache miss for key: {}", key);
			value = repository.findParam(key);
			cache.put(key, value);
			return value;
		}
	}

	/**
	 * Removes all of the entries from this cache. The cache will be empty after
	 * this call returns.
	 */
	public void clear() {
		clear(LocalDate.now());
	}

	private void clear(final LocalDate date) {
		log.debug("Clear");
		cache.clear();
		lastClearedDate = date;
	}

}
