package ch.dso.sedexinboxwatcher.kafka;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class NewSedexFileEvent {

    private String fileName;
    private String fileUrl;
    private String bucketName;

    public NewSedexFileEvent(String fileName, String fileUrl, String bucketName) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.bucketName = bucketName;
    }

}
