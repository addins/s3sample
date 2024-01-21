package org.addins.learn.tdd.s3sample;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.addins.learn.tdd.s3sample.domain.FileInfo;
import org.addins.learn.tdd.s3sample.repository.FileInfoRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureMockMvc
@Testcontainers
class FileResourceIT {

	public static final String SAMPLE_FILE_PATH = "/misc/img.png";
	private static final Logger log = LoggerFactory.getLogger(FileResourceIT.class);
	public static final String SAMPLE_FILE_NAME = "File-1.png";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AmazonS3 objectStorage;

	@Autowired
	private FileInfoRepository fileInfoRepository;

	@Value("${application.object-storage.bucket-name}")
	private String bucketName;

	private FileInfo fileInfo;

	public static final String MINIO_ACCESS_KEY = "minio";
	public static final String MINIO_SECRET_KEY = "minio123";
	@Container
	static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2023-06-09T07-32-12Z"))
			.withCommand("server /data")
			.withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY)
			.withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY)
			.withExposedPorts(9000);

	@DynamicPropertySource
	static void minioProps(DynamicPropertyRegistry registry) {
		Integer minioPort = minio.getMappedPort(9000);
		String minioHost = minio.getHost();
		registry.add("application.object-storage.service-endpoint", () -> "http://" + minioHost + ":" + minioPort);
		registry.add("application.object-storage.access-key", () -> MINIO_ACCESS_KEY);
		registry.add("application.object-storage.secret-key", () -> MINIO_SECRET_KEY);
	}

	@BeforeEach
	void setUp() {
		if (!objectStorage.doesBucketExistV2(bucketName)) {
			objectStorage.createBucket(bucketName);
		}
		boolean sampleFileExists = objectStorage.doesObjectExist(bucketName, SAMPLE_FILE_PATH);
		if (!sampleFileExists) {
			String file = Objects.requireNonNull(this.getClass().getResource("/samples/img.png")).getFile();
			PutObjectRequest por = new PutObjectRequest(bucketName, SAMPLE_FILE_PATH, file);
			objectStorage.putObject(por);
		}
		fileInfo = new FileInfo()
				.withId(UUID.randomUUID())
				.withName(SAMPLE_FILE_NAME)
				.withContentType("image/png")
				.withStorePath(SAMPLE_FILE_PATH);
		fileInfoRepository.saveAndFlush(fileInfo);
	}

	@Test
	@Transactional
	void getFilesAsList() throws Exception {
		mockMvc
				.perform(get("/api/files"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[*]").value(hasSize(1)))
				.andExpect(jsonPath("$.[*].id").value(hasItems(fileInfo.getId().toString())))
				.andExpect(jsonPath("$.[*].name").value(hasItems(SAMPLE_FILE_NAME)));
	}

	@Test
	@Transactional
	void getFile() throws Exception {
		ObjectMetadata fileInStoreMetadata = objectStorage.getObjectMetadata(bucketName, SAMPLE_FILE_PATH);
		mockMvc
				.perform(get("/api/files/{id}", fileInfo.getId()))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
				.andExpect(contentLengthIsEqualsWith(fileInStoreMetadata.getContentLength()))
				.andExpect(contentDispositionContainsRightFileName(fileInfo.getName()))
				.andExpect(cacheControlFor2Secs())
		;
	}

	@NotNull
	private static ResultMatcher contentLengthIsEqualsWith(long contentLength) {
		return header().longValue("Content-Length", contentLength);
	}

	@NotNull
	private static ResultMatcher cacheControlFor2Secs() {
		return header().string("Cache-Control", containsString("max-age=2,public"));
	}

	@NotNull
	private ResultMatcher contentDispositionContainsRightFileName(String name) {
		return header().string("Content-Disposition", containsString("inline; filename=\"" + name + "\""));
	}

	@Test
	@Transactional
	void storeFile() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file", "stored-img.png", IMAGE_JPEG_VALUE,
				this.getClass().getResourceAsStream("/samples/img.png"));

		mockMvc
				.perform(multipart("/api/files").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(not(nullValue())))
				.andExpect(jsonPath("$.name").value("stored-img.png"));

        assertThat(fileInfoRepository.findAll())
				.filteredOn(fi -> !fi.getId().equals(fileInfo.getId()))
				.hasSize(1)
				.first()
				.hasFieldOrPropertyWithValue("name", "stored-img.png")
				.satisfies(fileInfo -> {
					ObjectMetadata objectMetadata = objectStorage.getObjectMetadata(bucketName, fileInfo.getStorePath());
					assertThat(objectMetadata.getContentLength()).isEqualTo(file.getSize());
					assertThat(objectMetadata.getContentType()).isEqualTo(file.getContentType());
				});
	}

	@Test
	@Transactional
	void deleteFile() throws Exception {
		UUID id = fileInfo.getId();
		mockMvc
				.perform(delete("/api/files/{id}", id))
				.andExpect(status().isOk());

		Optional<FileInfo> fileInfoById = fileInfoRepository.findById(id);
		assertThat(fileInfoById).isEmpty();
		boolean fileExistsInStore = objectStorage.doesObjectExist(bucketName, SAMPLE_FILE_PATH);
		assertThat(fileExistsInStore).isFalse();
	}
}
