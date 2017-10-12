package com.greaterbank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.greaterbank.service.GreaterBankProcessService;

@SpringBootApplication
@EnableScheduling
public class GreaterBankBatchApplication extends SpringBootServletInitializer implements CommandLineRunner {
	
	@Autowired
	private GreaterBankProcessService greaterBankProcessService;

	public static void main(String[] args) {
		SpringApplication.run(GreaterBankBatchApplication.class, args);
	}
	
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(GreaterBankBatchApplication.class);
    }
	
	@Override
    public void run(String... strings) throws Exception {
		greaterBankProcessService.initSetUp();
	}
}
