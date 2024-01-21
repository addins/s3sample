package org.addins.learn.tdd.s3sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.addins.learn.tdd.s3sample.service.FileService;
import org.addins.learn.tdd.s3sample.service.dto.FileDTO;
import org.addins.learn.tdd.s3sample.service.dto.FileInfoDTO;
import org.springframework.core.io.InputStreamSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileResource {

    private final FileService fileService;

    @GetMapping("/files")
    public ResponseEntity<List<FileInfoDTO>> getFilesAsList(Pageable pageable) {
        log.debug("REST request to get list of files");
        Page<FileInfoDTO> files = fileService.getFiles(pageable);
        return ResponseEntity.ok(files.getContent());
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<InputStreamSource> getFile(@PathVariable UUID id) {
        log.debug("REST request to get file by id: {}", id);
        Optional<FileDTO> file = fileService.getFile(id);
        return file
                .map(fileDTO -> ResponseEntity
                        .ok()
                        .contentType(fileDTO.getContentType())
                        .contentLength(fileDTO.getContentLength())
                        .header("Content-Disposition",
                                ContentDisposition.inline().filename(fileDTO.getFileName()).build().toString())
						.header("Cache-Control", "max-age=2,public")
                        .body(fileDTO.getFileBinary()))
                .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
    }

    @PostMapping("/files")
    public ResponseEntity<FileInfoDTO> storeFile(MultipartFile file) {
        log.debug("REST request to store file: {}", file);
        Optional<FileInfoDTO> fileInfo = fileService.storeFile(file);
        return fileInfo.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        log.debug("REST request to delete file by id: {}", id);
        fileService.deleteFile(id);
        return ResponseEntity.ok().build();
    }
}
