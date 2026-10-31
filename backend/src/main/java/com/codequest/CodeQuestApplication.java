package com.codequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * Component scanning finds every @Service / @Repository / @RestController under com.codequest
 * and puts them in the IoC container (the "bean factory").
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CodeQuestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeQuestApplication.class, args);
    }
}
