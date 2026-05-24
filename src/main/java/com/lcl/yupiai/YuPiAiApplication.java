package com.lcl.yupiai;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class YuPiAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(YuPiAiApplication.class, args);
	}

}
