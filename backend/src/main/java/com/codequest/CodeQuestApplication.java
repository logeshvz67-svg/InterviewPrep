package com.codequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * Component scanning finds every @Service / @Repository / @RestController under com.codequest
 * and puts them in the IoC container (the "bean factory").
 */
// We log in with our own JWT flow, so Spring's default in-memory user is switched off
// (it printed "Using generated security password" at startup).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class CodeQuestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeQuestApplication.class, args);
    }
}
