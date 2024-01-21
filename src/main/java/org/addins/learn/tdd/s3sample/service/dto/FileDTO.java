package org.addins.learn.tdd.s3sample.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.io.InputStreamSource;
import org.springframework.http.MediaType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    private String fileName;
    private InputStreamSource fileBinary;
    private long contentLength = 0;
	private MediaType contentType;
}
