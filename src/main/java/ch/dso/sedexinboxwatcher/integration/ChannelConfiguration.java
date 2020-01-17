package ch.dso.sedexinboxwatcher.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class ChannelConfiguration {

    public final static String FROM_SFTP_CHANNEL_NAME = "fromSftpChannel";

    /**
     * Channel de réception des messages (fichiers) poller par le processus
     */
    @Bean
    public MessageChannel fromSftpChannel(){
        return new PublishSubscribeChannel();
    }

    @PostConstruct
    public void enableChannelDebug () {
        ((PublishSubscribeChannel)fromSftpChannel()).subscribe(message ->
                log.debug("[fromSftpChannel][messsage.handler] {}: ",message.getPayload()));
    }
}
