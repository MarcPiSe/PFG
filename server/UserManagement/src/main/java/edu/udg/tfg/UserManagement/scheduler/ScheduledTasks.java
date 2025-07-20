package edu.udg.tfg.UserManagement.scheduler;

import edu.udg.tfg.UserManagement.services.UserDeletionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;


@Component
public class ScheduledTasks {

    @Autowired
    private UserDeletionService userDeletionService;

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    @Scheduled(cron = "0 0 1 * * ?")
    public void retryAndCleanUserDeletions() {
        userDeletionService.retryPendingDeletions();
        userDeletionService.cleanupCompletedDeletions();
    }
}
