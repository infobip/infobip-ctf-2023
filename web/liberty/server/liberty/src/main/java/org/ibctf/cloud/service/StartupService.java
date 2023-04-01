package org.ibctf.cloud.service;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.ibctf.cloud.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.ibctf.cloud.Application.*;

@Singleton
public class StartupService implements ApplicationEventListener<StartupEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(StartupService.class);

    @Inject
    private NotificationService notificationService;
    @Inject
    private ScriptService scriptService;

    @Override
    public void onApplicationEvent(StartupEvent event) {
        try {
            if (Files.exists(FOLDER_DATA)) {
                FileSystemUtils.deleteRecursively(FOLDER_DATA);
            }

            for (Path p : Arrays.asList(FOLDER_DATA, FOLDER_FILES, FOLDER_CONFIGS, FOLDER_SCRIPTS)) {
                Files.createDirectory(p);
            }
            notificationService.notify(Notification.EventType.BOOT);
            scriptService.getResults().forEach((path, integer) -> {
                if (integer != 0) {
                    LOG.warn("{} exited with code {}", path, integer);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("cannot execute startup", e);
        }
    }
}
