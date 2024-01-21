package org.addins.learn.tdd.s3sample.repository;

import org.addins.learn.tdd.s3sample.domain.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, UUID> {
}
