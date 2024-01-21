package org.addins.learn.tdd.s3sample.service.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.addins.learn.tdd.s3sample.service.FileBinaryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileBinaryStoreS3Impl implements FileBinaryStore {

	@Value("${application.object-storage.bucket-name}")
	private String bucketName;

	private final AmazonS3 objectStorage;

	@Override
	public void delete(String path) {
		try {
			DeleteObjectRequest dor = new DeleteObjectRequest(
					bucketName,
					path
			);
			objectStorage.deleteObject(dor);
		} catch (AmazonServiceException ase) {
			log.error(
					"""
							Error Message:       {}
							HTTP Status Code:    {}
							AWS Error Code:      {}
							Error Type:          {}
							Request ID:          {}
							""",
					ase.getMessage(),
					ase.getStatusCode(),
					ase.getErrorCode(),
					ase.getErrorType(),
					ase.getRequestId()
			);
			throw ase;
		}
	}

	@Override
	public void store(String path, MultipartFile file) {
		try {
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentType(file.getContentType());
			objectMetadata.setContentLength(file.getSize());
			PutObjectRequest putObjectRequest = new PutObjectRequest(
					bucketName,
					path,
					file.getInputStream(),
					objectMetadata
			);
			objectStorage.putObject(putObjectRequest);
		} catch (AmazonServiceException ase) {
			log.error(
                    """
                            Error Message:       {}
                            HTTP Status Code:    {}
                            AWS Error Code:      {}
                            Error Type:          {}
                            Request ID:          {}""",
					ase.getMessage(),
					ase.getStatusCode(),
					ase.getErrorCode(),
					ase.getErrorType(),
					ase.getRequestId()
			);
			throw ase;
		} catch (AmazonClientException ace) {
			log.error("Error Message: {}", ace.getMessage());
			throw ace;
		} catch (IOException e) {
			log.error("Error storing file: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

	@Override
	public File getFile(String path) {
		try {
			GetObjectRequest getObjectRequest = new GetObjectRequest(
					bucketName,
					path
			);
			S3Object object = objectStorage.getObject(getObjectRequest);
			ObjectMetadata objectMetadata = object.getObjectMetadata();
			S3ObjectInputStream objectContent = object.getObjectContent();
			try {
				ByteArrayResource byteArrayResource = new ByteArrayResource(objectContent.readAllBytes());
				return new File(byteArrayResource, objectMetadata.getContentLength());
			} catch (IOException e) {
				log.error("Error retrieving file: {}, error: {}", path, e.getMessage());
				throw new IllegalStateException(e);
			}
		} catch (AmazonServiceException ase) {
			log.error(
					"""
							Error Message:       {}
							HTTP Status Code:    {}
							AWS Error Code:      {}
							Error Type:          {}
							Request ID:          {}""",
					ase.getMessage(),
					ase.getStatusCode(),
					ase.getErrorCode(),
					ase.getErrorType(),
					ase.getRequestId()
			);
			throw new IllegalStateException(ase);
		} catch (AmazonClientException ace) {
			log.error("Error Message: {}", ace.getMessage());
			throw new IllegalStateException(ace);
		}
	}
}
