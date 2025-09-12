package com.ddak.yongha.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "mailExecutor")
	public Executor mailExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(10);
		ex.setQueueCapacity(100);
		ex.setThreadNamePrefix("mail-");
		ex.initialize();
		return ex;
	}
	
    @Bean(name = "pythonExecutor")
    public Executor pythonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);     // 최소 스레드
        executor.setMaxPoolSize(10);     // 최대 스레드
        executor.setQueueCapacity(100);  // 큐 용량
        executor.setThreadNamePrefix("python-");
        executor.initialize();
        return executor;
    }
}
