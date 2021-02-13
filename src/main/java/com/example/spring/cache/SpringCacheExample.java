package com.example.spring.cache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ComponentScan
@PropertySource("classpath:application.properties")
public class SpringCacheExample {

	public static void main(final String... args) throws InterruptedException {
		try (final ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(SpringCacheExample.class)) {
			System.out.println("============================================================================");
			final ParamCache cache = context.getBean(ParamCache.class);

			final Runnable runnable = () -> {
				for (int i = 0; i < 10; i++) {
					log.info("{} -> {}", i, cache.get(Integer.toString(i)));
				}
				for (int i = 0; i < 7; i++) {
					log.info("{} -> {}", i, cache.get(Integer.toString(i)));
				}
				for (int i = 0; i < 5; i++) {
					log.info("{} -> {}", i, cache.get(Integer.toString(i)));
				}
				for (int i = 0; i < 2; i++) {
					for (int j = 1; j < 10; j++) {
						log.info("{} -> {}", i, cache.get(Integer.toString(i)));
					}
				}
				for (int i = 0; i < 15; i++) {
					log.info("{} -> {}", i, cache.get(Integer.toString(i)));
				}
			};

			final ExecutorService executorService = Executors.newCachedThreadPool();
			executorService.execute(runnable);
			TimeUnit.SECONDS.sleep(5);
			executorService.execute(runnable);
			executorService.shutdown();
			executorService.awaitTermination(1, TimeUnit.MINUTES);
		}
		finally {
			System.out.println("============================================================================");
		}
	}

}
