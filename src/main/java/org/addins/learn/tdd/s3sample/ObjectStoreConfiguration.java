package org.addins.learn.tdd.s3sample;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class ObjectStoreConfiguration {

	@Value("${application.object-storage.service-endpoint}")
	private String serviceEndpoint;

	@Value("${application.object-storage.access-key}")
	private String accessKey;

	@Value("${application.object-storage.secret-key}")
	private String secretKey;

	@Value("${application.object-storage.type:MINIO}")
	private String type;

	@Value("${application.object-storage.region:us-east-1}")
	private String regionName;

	@Bean
	public AmazonS3 objectStorage() {
		AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
		ClientConfiguration clientConfiguration = new ClientConfiguration();

		String region;
		if (Objects.equals(type, "MINIO")) {
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			region = Regions.US_EAST_1.name();
		} else {
			region = this.regionName;
		}

		return AmazonS3ClientBuilder
				.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();
	}
}
