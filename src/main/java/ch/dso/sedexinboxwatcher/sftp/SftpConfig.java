package ch.dso.sedexinboxwatcher.sftp;

import com.jcraft.jsch.ChannelSftp;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

@Slf4j
@Getter
@Configuration
public class SftpConfig {
    @Value("${sftp.host}")
    private String sftpHost;
    @Value("${sftp.port}")
    private int sftpPort;
    @Value("${sftp.user}")
    private String sftpUser;
    @Value("${sftp.privateKey:#{null}}")
    private Resource sftpPrivateKey;
    @Value("${sftp.privateKeyPassphrase:}")
    private String sftpPrivateKeyPassphrase;
    @Value("${sftp.password}")
    private String sftpPasword;
    @Value("${sftp.remote.directory.download}")
    private String sftpRemoteDirectoryDownload;
    @Value("${sftp.local.directory.download}")
    private String sftpLocalDirectoryDownload;
    @Value("${sftp.remote.directory.download.filter}")
    private String sftpRemoteDirectoryDownloadFilter;
    @Value("${sftp.remote.directory.upload}")
    private String sftpRemoteDirectoryUpload;

    private final static boolean DELETE_SEDEX_FILE = Boolean.TRUE;


    /**
     * Sessionfactory poour la connection au serveur sftp
     * @return une instance de SessionFactory paramétrée
     */
    @Bean
    public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost(sftpHost);
        factory.setPort(sftpPort);
        factory.setUser(sftpUser);
        if (sftpPrivateKey != null) {
            factory.setPrivateKey(sftpPrivateKey);
            factory.setPrivateKeyPassphrase(sftpPrivateKeyPassphrase);
        } else {
            factory.setPassword(sftpPasword);
        }
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(factory);
    }



    /**
     * Synchronization des répertoires tmp et Sftp
     * @return une instance de SftpFileSynchronizer
     */
    @Bean
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(DELETE_SEDEX_FILE);
        fileSynchronizer.setRemoteDirectory(sftpRemoteDirectoryDownload);
        fileSynchronizer
                .setFilter(new SftpSimplePatternFileListFilter(sftpRemoteDirectoryDownloadFilter));
        return fileSynchronizer;
    }









}
