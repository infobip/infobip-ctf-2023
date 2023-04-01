package org.ibctf.cloud.service;

import jakarta.inject.Singleton;
import org.ibctf.cloud.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
@Singleton
public class ScriptService {

    private static final Logger LOG = LoggerFactory.getLogger(ScriptService.class);

    private Map<Path, Integer> results;

    public ScriptService() throws Exception {
        this.results = new HashMap<>();
        runAll();
    }

    public void runAll() throws IOException {
        if (!Files.exists(Application.FOLDER_SCRIPTS)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(Application.FOLDER_SCRIPTS)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder("/bin/sh", path.toString());
                    Process p = pb.start();
                    p.waitFor(3, TimeUnit.SECONDS);
                    results.put(path, p.exitValue());
                } catch (Exception e) {
                    LOG.error("execution failed: {} {}", path, e.getMessage());
                }
            });
        }
    }

    public Map<Path, Integer> getResults() {
        return results;
    }
}
