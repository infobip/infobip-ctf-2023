package org.ibctf.cloud.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.annotation.Repository;
import org.ibctf.cloud.model.FileTopic;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface FileTopicRepository extends CrudRepository<FileTopic, Long> {

    Optional<FileTopic> findByTopic(String topic);
}
