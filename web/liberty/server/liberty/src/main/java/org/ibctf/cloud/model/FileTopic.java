package org.ibctf.cloud.model;

import javax.persistence.*;

@Entity
@Table(name = "file_topic")
public class FileTopic {

    @Id
    @GeneratedValue
    @Column(name = "file_topic_id")
    private Long id;

    @Column(name = "topic")
    private String topic;

    public FileTopic() {
    }

    public FileTopic(String topic) {
        this.topic = topic;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
