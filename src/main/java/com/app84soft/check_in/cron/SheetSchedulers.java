// src/main/java/com/app84soft/check_in/cron/SheetSchedulers.java
package com.app84soft.check_in.cron;

import com.app84soft.check_in.services.sheet.BootstrapService;
import com.app84soft.check_in.services.sheet.GhnBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("sheetSchedulersCron")
@RequiredArgsConstructor
@Slf4j
public class SheetSchedulers {

    private final BootstrapService bootstrap;
    private final GhnBackfillService backfill;

    /** 5 phút/lần: backfill GHN còn thiếu */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void backfillGhnMissing() {
        int n = backfill.backfillMissing(200);
        if (n > 0) log.info("Backfilled {} GHN orders", n);
    }

    /** 10 phút/lần: full-sync 7 ngày gần đây (async) */
    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 30 * 1000L)
    public void syncRecent() {
        bootstrap.triggerFullSyncAsync();
    }
}
