package ch.dso.sedexinboxwatcher.integration;

import ch.dso.sedexinboxwatcher.kafka.KafkaProducerConfiguration;
import ch.dso.sedexinboxwatcher.kafka.NewSedexFileEvent;
import ch.dso.sedexinboxwatcher.minio.MinioConfig;
import ch.dso.sedexinboxwatcher.sftp.SftpConfig;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.MessageHandler;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Classe implémentant le processus de polling/synchronisation/notification basé sur Spring Integration
 */
@Slf4j
@Configuration
public class NotificationProcessConfiguration {

    @Autowired
    SftpConfig sftpConfig;
    @Autowired
    KafkaProducerConfiguration kafkaProducerConfiguration;
    @Autowired
    MinioClient minioClient;
    @Autowired
    KafkaTemplate kafkaTemplate;

    private final static boolean DELETE_TMP_FILE = Boolean.TRUE;

    /* CRON EXPRESSION EXAMPLE */
    private final static String CRON_EACH_MINUTES = "0 * * * * *";
    private final static String CRON_EACH_SECONDS = "* * * * * *";
    private final static String CRON_EACH_20_SECONDS = "*/20 * * * * *";



    /**
     * Point d'entrée du traitement de polling/synchronisation. les messages sont publiés sur le channel fromSftpChannel
     * @return une instance de SftpInboundFileSynchornizongMessageSource
     */
    @Bean
    @InboundChannelAdapter(channel = ChannelConfiguration.FROM_SFTP_CHANNEL_NAME, poller = @Poller(cron = CRON_EACH_20_SECONDS))
    public MessageSource<File> sftpMessageSource() {
        log.info("Inbound polling starting....");

        SftpInboundFileSynchronizingMessageSource source = new SftpInboundFileSynchronizingMessageSource(
                sftpConfig.sftpInboundFileSynchronizer());
        source.setLocalDirectory(new File(sftpConfig.getSftpLocalDirectoryDownload()));
        source.setAutoCreateLocalDirectory(true); //création auto du répertoire local
        source.setLocalFilter(new AcceptOnceFileListFilter<>());
        return source;
    }

    @Bean
    @ServiceActivator(inputChannel = ChannelConfiguration.FROM_SFTP_CHANNEL_NAME)
    public MessageHandler sendToMinio() {

        testOrCreateBucket();

        return message -> {

            File nouveauFichier = (File) message.getPayload();

            log.info("Nouveau fichier [sedex inbox]: " + nouveauFichier);

            try {

                pushToMinio(nouveauFichier);

                deleteTmpFile(nouveauFichier);

                pushEventToKafka(nouveauFichier);

            } catch (InvalidBucketNameException e) {
                log.error(e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private void pushEventToKafka(File nouveauFichier) throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException, InternalException, InvalidResponseException {
        String fileUrl =  minioClient.getObjectUrl(MinioConfig.SEDEX_INBOX_BUCKET_NAME,nouveauFichier.getName());
        NewSedexFileEvent newSedexFileEvent = new NewSedexFileEvent(nouveauFichier.getName(),fileUrl,MinioConfig.SEDEX_INBOX_BUCKET_NAME);

        kafkaTemplate.send(kafkaProducerConfiguration.getSedexInboxTopic(),newSedexFileEvent);
        log.info("[sendToKafka]: " + newSedexFileEvent);
    }

    private void deleteTmpFile(File nouveauFichier) {
        if(DELETE_TMP_FILE){
            nouveauFichier.delete();
        }
    }

    private void pushToMinio(File nouveauFichier) throws InvalidBucketNameException, NoSuchAlgorithmException, IOException, InvalidKeyException, NoResponseException, XmlPullParserException, ErrorResponseException, InternalException, InvalidArgumentException, InsufficientDataException, InvalidResponseException {
        minioClient.putObject(MinioConfig.SEDEX_INBOX_BUCKET_NAME,
                nouveauFichier.getName(),
                nouveauFichier.getAbsolutePath(),
                nouveauFichier.length(),
                new HashMap<>(),
                null,
                null);
    }

    private void testOrCreateBucket() {
        try {
            boolean bucketExist = minioClient.bucketExists(MinioConfig.SEDEX_INBOX_BUCKET_NAME);

            if(bucketExist){
                log.info("Bucket {} Already Exist", MinioConfig.SEDEX_INBOX_BUCKET_NAME);
            }else{
                log.info("Bucket {} don't already exist. Try to create it.", MinioConfig.SEDEX_INBOX_BUCKET_NAME);
                minioClient.makeBucket(MinioConfig.SEDEX_INBOX_BUCKET_NAME);
                log.info("Bucket Created");
            }

        } catch (InvalidBucketNameException | NoSuchAlgorithmException | IOException | InvalidKeyException | NoResponseException
                | XmlPullParserException | ErrorResponseException | InternalException | RegionConflictException | InsufficientDataException | InvalidResponseException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }


}
