package com.lcl.myaiagent;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
@EnableAsync
public class MyAiAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyAiAgentApplication.class, args);
	}

}
