package org.addins.learn.tdd.s3sample.service;

import lombok.RequiredArgsConstructor;
import org.addins.learn.tdd.s3sample.domain.FileInfo;
import org.addins.learn.tdd.s3sample.repository.FileInfoRepository;
import org.addins.learn.tdd.s3sample.service.dto.FileDTO;
import org.addins.learn.tdd.s3sample.service.dto.FileInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

	private final FileInfoRepository fileInfoRepository;
	private final FileBinaryStore fileBinaryStore;

	public Page<FileInfoDTO> getFiles(Pageable pageable) {
		return fileInfoRepository.findAll(pageable)
				.map(fi -> new FileInfoDTO(fi.getId(), fi.getName()));
	}

	@Transactional
	public Optional<FileInfoDTO> storeFile(MultipartFile file) {
		UUID id = UUID.randomUUID();
		FileInfo fileInfo = new FileInfo()
				.withId(id)
				.withContentType(file.getContentType())
				.withName(file.getOriginalFilename())
				.withStorePath("/misc/" + id);
		fileInfoRepository.save(fileInfo);
		fileBinaryStore.store(fileInfo.getStorePath(), file);
		return Optional.of(new FileInfoDTO(fileInfo.getId(), fileInfo.getName()));
	}

	@Transactional
	public void deleteFile(UUID id) {
		fileInfoRepository.findById(id)
				.ifPresent(fileInfo -> {
					fileInfoRepository.deleteById(id);
					fileBinaryStore.delete(fileInfo.getStorePath());
				});
	}

	public Optional<FileDTO> getFile(UUID id) {
		return fileInfoRepository.findById(id)
				.map(fi -> {
					FileBinaryStore.File file = fileBinaryStore.getFile(fi.getStorePath());
					return new FileDTO(
							fi.getName(),
							file.getInputStreamSource(),
							file.getContentLength(),
							MediaType.parseMediaType(fi.getContentType())
					);
				});
	}
}
