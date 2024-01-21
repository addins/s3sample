package org.addins.learn.tdd.s3sample.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.core.io.InputStreamSource;
import org.springframework.web.multipart.MultipartFile;

public interface FileBinaryStore {

    void delete(String path);

    void store(String path, MultipartFile file);

    File getFile(String path);

	@Data
	@AllArgsConstructor
	class File {

		private InputStreamSource inputStreamSource;
		private long contentLength;
	}
}
