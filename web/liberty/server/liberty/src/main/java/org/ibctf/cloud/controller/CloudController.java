package org.ibctf.cloud.controller;

import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.inject.Inject;
import net.bytebuddy.utility.RandomString;
import org.ibctf.cloud.Application;
import org.ibctf.cloud.model.FileMetadata;
import org.ibctf.cloud.model.FileTopic;
import org.ibctf.cloud.model.Notification;
import org.ibctf.cloud.repository.FileMetadataRepository;
import org.ibctf.cloud.repository.FileTopicRepository;
import org.ibctf.cloud.service.NotificationService;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
public class CloudController {

    @Inject
    private FileTopicRepository fileTopicRepository;
    @Inject
    private FileMetadataRepository fileMetadataRepository;
    @Inject
    private NotificationService notificationService;

    @Get("/")
    public HttpResponse index() {
        return HttpResponse.seeOther(URI.create("/cloud"));
    }

    @Get("/cloud")
    public HttpResponse cloud() {
        String uuid = UUID.randomUUID().toString();
        fileTopicRepository.save(new FileTopic(uuid));
        return HttpResponse.created(Map.of("path", String.format("/ws/%s", uuid)));
    }

    @Post(value = "/cloud/config", consumes = {MediaType.APPLICATION_YAML})
    public HttpResponse config(@Body byte[] payload, HttpRequest request) {
        InetSocketAddress addr = request.getRemoteAddress();
        if (!addr.getAddress().isLoopbackAddress()) {
            return HttpResponse.unauthorized();
        }

        Yaml yaml = new Yaml();
        Notification n = yaml.load(new String(payload));
        if (n.getNotifyUrl() == null || n.getNotifyUrl().isBlank()) {
            HttpResponse.badRequest(Map.of("message", "notifyUrl is empty"));
        }
        if (n.getMethod() == null) {
            n.setMethod(HttpMethod.GET);
        }
        if (n.getType() == null) {
            n.setType(Notification.EventType.BOOT);
        }

        String name = RandomString.make(14);
        try {
            Files.write(Paths.get(Application.FOLDER_CONFIGS.toString(), name), payload);
            notificationService.notify(Notification.EventType.CONFIG);
        } catch(IOException e) {
            return HttpResponse.serverError(e.getMessage());
        }
        return HttpResponse.created(Map.of("name", name));
    }

    @Post(value = "/cloud/script", consumes = {MediaType.APPLICATION_OCTET_STREAM})
    public HttpResponse script(@Body byte[] payload, HttpRequest request) {
        InetSocketAddress addr = request.getRemoteAddress();
        if (!addr.getAddress().isLoopbackAddress()) {
            return HttpResponse.unauthorized();
        }

        String name = RandomString.make(14);
        try {
            Files.write(Paths.get(Application.FOLDER_SCRIPTS.toString(), name), payload);
            notificationService.notify(Notification.EventType.SCRIPT);
        } catch(IOException e) {
            return HttpResponse.serverError(e.getMessage());
        }
        return HttpResponse.created(Map.of("name", name));
    }

    @Post(value = "/cloud/{uuid}", consumes = {MediaType.IMAGE_PNG})
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse upload(@Body byte[] payload, String uuid) {
        if (!checkHeader(payload, PNG_HEADER)) {
            return HttpResponse.badRequest(Map.of("message", "only PNG supported"));
        }

        Optional<FileMetadata> optional = fileMetadataRepository.findByUuid(uuid);
        if (optional.isEmpty()) {
            return HttpResponse.badRequest(Map.of("message", "unknown UUID"));
        }

        FileMetadata fm = optional.get();
        if (fm.isUploaded()) {
            return HttpResponse.badRequest(Map.of("message", "already uploaded"));
        }
        try {
            Files.write(Paths.get(Application.FOLDER_FILES.toString(), fm.getName()), payload);
            fm.setUploaded(true);
            fileMetadataRepository.update(fm);
            notificationService.notify(Notification.EventType.UPLOAD);
        } catch(IOException e) {
            return HttpResponse.serverError(e.getMessage());
        }
        return HttpResponse.created(fm);
    }

    private static final byte[] PNG_HEADER = new byte[]{(byte)0x89, 0x50, 0x4e, 0x47};

    private boolean checkHeader(byte[] target, byte[] against) {
        if (target == null || against == null) {
            return false;
        }
        if (against.length > target.length) {
            return false;
        }
        for (int i = 0; i < against.length; ++i) {
            if (against[i] != target[i]) {
                return false;
            }
        }
        return true;
    }
}
