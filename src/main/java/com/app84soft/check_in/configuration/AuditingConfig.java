package com.app84soft.check_in.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
//import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableJpaAuditing
//@EnableMongoAuditing
public class AuditingConfig {
    // That's all here for now. We'll add more auditing configurations later.
}
