package edu.udg.tfg.Trash.scheduler;

import edu.udg.tfg.Trash.services.TrashService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;


@Component
public class ScheduledTasks {

    @Autowired
    private TrashService trashService;

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Scheduled(cron = "0 0 0 * * ?")
    public void markExpiredRecordsAsPending() {
        trashService.markExpiredAsPending();
    }

    @Scheduled(fixedRate = 300000)
    public void processPendingDeletions() {
        trashService.processPendingDeletions();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void clean() {
        trashService.cleanRecords();
    }
}
