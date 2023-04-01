package org.ibctf.cloud.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.annotation.Repository;
import org.ibctf.cloud.model.FileMetadata;
import org.ibctf.cloud.model.FileTopic;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface FileMetadataRepository extends CrudRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByUuid(String uuid);
    List<FileMetadata> findByTopic(FileTopic topic);
}
