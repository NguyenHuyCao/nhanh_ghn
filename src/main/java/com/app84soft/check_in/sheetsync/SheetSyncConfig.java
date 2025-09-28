package com.app84soft.check_in.sheetsync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sheetsync")
@Data
public class SheetSyncConfig {
    private boolean enabled = true;
    private Poll poll = new Poll();
    private Window window = new Window();

    @Data public static class Poll {
        private long nhanhFixedDelayMs = 30000;
        private long ghnFixedDelayMs   = 30000;
    }
    @Data public static class Window {
        private int  nhanhMinutes = 20;  // cửa sổ trượt Nhanh
        private int  ghnHours     = 48;  // cửa sổ GHN
    }
}
