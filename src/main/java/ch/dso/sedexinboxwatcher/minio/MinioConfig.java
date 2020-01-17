package ch.dso.sedexinboxwatcher.minio;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.server.endpoint}")
    private String endpoint;

    @Value("${minio.server.accessKey}")
    private String accessKey;

    @Value("${minio.server.secretKey}")
    private String secretKey;

    public final static String SEDEX_INBOX_BUCKET_NAME = "sedex-inbox2";

    @Bean
    public MinioClient minioClient () throws InvalidPortException, InvalidEndpointException {
        MinioClient minioClient = new MinioClient(endpoint, accessKey, secretKey);
        return minioClient;
    }
}
