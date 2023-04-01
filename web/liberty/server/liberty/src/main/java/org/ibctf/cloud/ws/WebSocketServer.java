package org.ibctf.cloud.ws;

import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.CloseReason;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;
import jakarta.inject.Inject;
import org.ibctf.cloud.Application;
import org.ibctf.cloud.model.FileMetadata;
import org.ibctf.cloud.model.FileTopic;
import org.ibctf.cloud.repository.FileMetadataRepository;
import org.ibctf.cloud.repository.FileTopicRepository;
import org.reactivestreams.Publisher;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ServerWebSocket("/ws/{topic}")
@Secured(SecurityRule.IS_ANONYMOUS)
public class WebSocketServer {

    private static final WebSocketResponse RESP_NOT_FOUND = new WebSocketResponse<>(WebSocketStatus.ERROR, "Not found");

    @Inject
    private FileTopicRepository fileTopicRepository;
    @Inject
    private FileMetadataRepository fileMetadataRepository;

    @OnOpen
    public Publisher<WebSocketResponse> onOpen(String topic, WebSocketSession session) {
        Optional<FileTopic> optional = fileTopicRepository.findByTopic(topic);
        if (optional.isEmpty()) {
            session.sendAsync(new WebSocketResponse<>(WebSocketStatus.ERROR, "Unknown topic"))
                    .whenComplete((s, throwable) -> session.close(CloseReason.BAD_GATEWAY));
            return null;
        }
        return session.send(new WebSocketResponse<>(WebSocketStatus.OK, "Connected"));
    }

    @OnMessage
    public Publisher<WebSocketResponse> onMessage(String topic, @Valid WebSocketRequest message, WebSocketSession session) {
        WebSocketResponse r = null;
        switch (message.getRequestType()) {
            case WebSocketRequest.REQUEST_NEW_FILE -> r = newFile(topic, message);
            case WebSocketRequest.REQUEST_DELETE_FILE -> r = deleteFile(message);
            case WebSocketRequest.REQUEST_ABORT -> r = abort(message);
            case WebSocketRequest.REQUEST_LIST_FILES -> r = listFiles(topic);
            case WebSocketRequest.REQUEST_LIST_UUID -> r = listUuid(message);
            case WebSocketRequest.REQUEST_QUIT -> {
                Publisher<WebSocketResponse> resp = session.send(new WebSocketResponse<>(WebSocketStatus.OK, "Disconnecting"));
                session.close(CloseReason.NORMAL);
                return resp;
            }
        }
        return session.send(r);
    }

    @OnClose
    public void onClose(String topic, WebSocketSession session) {
        if (session != null && session.isOpen() && !session.isEmpty()) {
            session.close(CloseReason.NORMAL);
        }
    }

    @OnError
    public void onError(String topic, WebSocketSession session) {
        session.send(new WebSocketResponse<>(WebSocketStatus.ERROR, "error occurred"));
    }

    private WebSocketResponse newFile(String topic, WebSocketRequest message) {
        Optional<FileTopic> optional = fileTopicRepository.findByTopic(topic);
        if (optional.isEmpty()) {
            return RESP_NOT_FOUND;
        }

        Path p = Path.of(Application.FOLDER_FILES.toString(), message.getMessage());
        if (!p.normalize().toAbsolutePath().startsWith(Application.FOLDER_DATA.toAbsolutePath())) {
            return new WebSocketResponse<>(WebSocketStatus.ERROR, "Invalid name");
        }
        if (Files.exists(p)) {
            return new WebSocketResponse<>(WebSocketStatus.ERROR, "Already exists");
        }

        String uuid = UUID.randomUUID().toString();
        FileMetadata fm = new FileMetadata(message.getMessage(), uuid, optional.get());
        fileMetadataRepository.save(fm);
        return new WebSocketResponse<>(WebSocketStatus.OK, uuid);
    }

    private WebSocketResponse deleteFile(WebSocketRequest message) {
        Optional<FileMetadata> optional = fileMetadataRepository.findByUuid(message.getMessage());
        if (optional.isEmpty()) {
            return RESP_NOT_FOUND;
        }

        FileMetadata fm = optional.get();
        if (!fm.isUploaded()) {
            return new WebSocketResponse<>(WebSocketStatus.ERROR, "File not uploaded");
        }

        try {
            if (!Files.deleteIfExists(Path.of(fm.getName()))) {
                return new WebSocketResponse<>(WebSocketStatus.ERROR, "Delete failed");
            }
            fileMetadataRepository.delete(fm);
        } catch (IOException e) {
            return new WebSocketResponse<>(WebSocketStatus.ERROR, "Internal error");
        }

        return new WebSocketResponse<>(WebSocketStatus.OK, fm.getUuid());
    }

    private WebSocketResponse abort(WebSocketRequest message) {
        Optional<FileMetadata> optional = fileMetadataRepository.findByUuid(message.getMessage());
        if (optional.isEmpty()) {
            return RESP_NOT_FOUND;
        }

        FileMetadata fm = optional.get();
        if (fm.isUploaded()) {
            return new WebSocketResponse<>(WebSocketStatus.ERROR, "File already uploaded");
        }
        fileMetadataRepository.delete(fm);
        return new WebSocketResponse<>(WebSocketStatus.OK, fm.getUuid());
    }

    private WebSocketResponse listFiles(String topic) {
        Optional<FileTopic> optional = fileTopicRepository.findByTopic(topic);
        if (optional.isEmpty()) {
            return RESP_NOT_FOUND;
        }

        List<FileMetadata> fmList = fileMetadataRepository.findByTopic(optional.get());
        return new WebSocketResponse<>(WebSocketStatus.OK, fmList);
    }

    private WebSocketResponse listUuid(WebSocketRequest message) {
        Optional<FileMetadata> optional = fileMetadataRepository.findByUuid(message.getMessage());
        return optional.map(fileMetadata -> new WebSocketResponse<>(WebSocketStatus.OK, fileMetadata)).orElse(RESP_NOT_FOUND);
    }
}
