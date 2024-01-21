package org.addins.learn.tdd.s3sample.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDTO {
	private UUID id;

	private String name;
}
