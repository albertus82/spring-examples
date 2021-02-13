package com.example.spring.cache;

import static com.example.spring.cache.ParamType.P1;
import static com.example.spring.cache.ParamType.P2;
import static com.example.spring.cache.ParamType.P3;
import static com.example.spring.cache.ParamType.P4;
import static com.example.spring.cache.ParamType.P5;
import static com.example.spring.cache.ParamType.P6;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ParamRepository {

	private final Random random = new Random();

	public Map<ParamType, Long> findParam(String key) {
		log.warn("Database access for key: {}", key);
		try {
			TimeUnit.MILLISECONDS.sleep(1000); // simulate database access
		}
		catch (final InterruptedException e) {
			log.warn("Interrupted!", e);
			Thread.currentThread().interrupt();
		}
		final Map<ParamType, Long> map = new EnumMap<>(ParamType.class);
		map.put(P1, Long.valueOf(random.nextInt(100)));
		map.put(P2, Long.valueOf(random.nextInt(100)));
		map.put(P3, Long.valueOf(random.nextInt(100)));
		map.put(P4, Long.valueOf(random.nextInt(100)));
		map.put(P5, Long.valueOf(random.nextInt(100)));
		map.put(P6, Long.valueOf(random.nextInt(100)));
		return map;
	}

}
