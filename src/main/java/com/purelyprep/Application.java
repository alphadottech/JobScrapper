package com.purelyprep;

import com.purelyprep.services.JobScraper;
import com.purelyprep.services.JobScraperWorker;
import com.purelyprep.services.ScheduleService;
import com.purelyprep.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Value("${redis.job.queue}")
    private String jobQueue;

    @Value("${environment.name}")
    public String environmentName;

    @Value("${google.sheets.service.key}")
    private String googleSheetsServiceKey;

    @Value("${google.sheets.service.key.name}")
    private String googleSheetsServiceKeyName;

    @Bean
    Boolean startUp() {
        log.info("Environment: [" + environmentName + "]");

//        (new Thread(() -> {
//            try {
//                Thread.sleep(3000);
//                JobScraper jobScraper = new JobScraper(0, this.getRestTemplate());
//                log.info(jobScraper.processJobDetails("https://www.linkedin.com/jobs-guest/jobs/api/jobPosting/3954180377", 0).toString());
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        })).start();

        return true;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(JobScraperWorker worker) {
        return new MessageListenerAdapter(worker, "receiveMessage");
    }

    @Bean
    RedisMessageListenerContainer container(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter listenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(jobQueue));
        return container;
    }

    @Bean RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Qualifier("GoogleSheetsKey")
    File getKeyFile() {
        return Util.decodeBase64ToFile(googleSheetsServiceKeyName, googleSheetsServiceKey);
    }

    @Bean
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("purelyprep-");
        executor.initialize();
        return executor;
    }

}
