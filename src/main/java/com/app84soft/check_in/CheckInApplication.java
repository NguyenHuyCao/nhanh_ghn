package com.app84soft.check_in;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

@EntityScan(basePackageClasses = {CheckInApplication.class, Jsr310JpaConverters.class})
@SpringBootApplication
@ConfigurationPropertiesScan
public class CheckInApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckInApplication.class, args);
    }

}
