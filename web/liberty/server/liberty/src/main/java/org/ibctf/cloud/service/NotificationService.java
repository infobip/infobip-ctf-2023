package org.ibctf.cloud.service;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.simple.SimpleHttpRequest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.ibctf.cloud.Application;
import org.ibctf.cloud.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
@Singleton
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    @Inject
    private HttpClient httpClient;

    public void notify(Notification.EventType event) throws IOException {
        try (Stream<Path> paths = Files.walk(Application.FOLDER_CONFIGS)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Yaml yaml = new Yaml();
                    FileReader fr = new FileReader(path.toAbsolutePath().toString());
                    Notification n = yaml.load(fr);
                    fr.close();
                    if (n.getType() == event) {
                        HttpRequest request = new SimpleHttpRequest<>(n.getMethod(), n.getNotifyUrl(), n.getType());
                        httpClient.exchange(request);
                    }
                } catch (Exception e) {
                    LOG.error("failed to notify: {} {}", path, e.getMessage());
                }
            });
        }
    }
}
