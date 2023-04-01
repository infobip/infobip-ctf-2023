package org.ibctf.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue
    @Column(name = "file_metadata_id")
    @JsonIgnore
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "uploaded")
    private boolean uploaded;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinColumn(name = "file_topic_id", nullable = false)
    @JsonIgnore
    private FileTopic topic;

    public FileMetadata() {
    }

    public FileMetadata(String name, String uuid, FileTopic topic) {
        this.name = name;
        this.uuid = uuid;
        this.topic = topic;
    }

    public FileMetadata(Long id, String name, String uuid, boolean uploaded, FileTopic topic) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
        this.uploaded = uploaded;
        this.topic = topic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public FileTopic getTopic() {
        return topic;
    }

    public void setTopic(FileTopic topic) {
        this.topic = topic;
    }
}
