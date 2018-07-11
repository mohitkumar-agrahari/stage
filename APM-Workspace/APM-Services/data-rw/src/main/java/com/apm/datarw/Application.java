package com.apm.datarw;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.apm.datarw.config.SSTConfigurations;
import com.apm.datarw.cors.CustomCorsFilter;
import com.apm.historian.dac.util.HistorianUtil;

@SpringBootApplication
@EnableAutoConfiguration
@EntityScan("com.apm.datarw.entity.**")
@EnableJpaRepositories("com.apm.datarw.repo.**")
@ComponentScan("com.apm.**")
@Import({CustomCorsFilter.class})
@EnableScheduling
@EnableAsync
public class Application {
	private final Logger log = LoggerFactory.getLogger(Application.class);
	private static HistorianUtil historianUtil = null;
	@Bean(name="processExecutor")
	public TaskExecutor workExecutor() {
		log.info("Initializing thread executor. [Prefix: Analytic-Scheduler]");
		ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
		threadPoolTaskExecutor.setThreadNamePrefix("Analytic-Scheduler-");
		threadPoolTaskExecutor.setCorePoolSize(3);
		threadPoolTaskExecutor.setMaxPoolSize(3);
		threadPoolTaskExecutor.setQueueCapacity(600);
		threadPoolTaskExecutor.afterPropertiesSet();
		return threadPoolTaskExecutor;
	}

	@Autowired
	Environment env;

	@Autowired
	SSTConfigurations sstConfig;


	@Bean
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@PostConstruct
	private void loadDLL() throws Exception{
		log.info("Loading DLLS. Adding path to java.library.path :: "+ sstConfig.getDllPath());
		//Add java.library.path
		System.setProperty("java.library.path", sstConfig.getDllPath()+File.pathSeparator+System.getProperty("java.library.path"));
		//Set sys_paths to null
		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
		sysPathsField.setAccessible(true);
		sysPathsField.set(null, null);
		//Show java.library.path
		System.out.println(">>>>---------- Java Library Path ---------->>>>> " + System.getProperty("java.library.path"));
		if(Objects.nonNull(env.getActiveProfiles())){
			if(Arrays.asList(env.getActiveProfiles()).contains("local")){
				//Do stufff on local profile.
			}
		}
		Application.historianUtil = new HistorianUtil();

	}
	
	public static HistorianUtil getHistorianUtil(){
		return historianUtil;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class,args);
	}
}
