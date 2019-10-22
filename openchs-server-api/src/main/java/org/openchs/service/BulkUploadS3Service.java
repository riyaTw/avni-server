package org.openchs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

import static java.lang.String.format;

@Service
public class BulkUploadS3Service {

    private S3Service s3Service;

    @Autowired
    public BulkUploadS3Service(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public String uploadFile(MultipartFile source, String uuid) throws IOException {
        String targetFileName = format("%s-%s", uuid, source.getOriginalFilename());
        return s3Service.uploadFile(source, targetFileName, "bulkuploads/input");
    }

    public String uploadErrorFile(File tempSourceFile, String uuid) throws IOException {
        return s3Service.uploadFile(tempSourceFile, format("%s.csv", uuid), "bulkuploads/error");
    }

    public File getLocalErrorFile(String uuid) {
        File errorDir = new File(format("%s/bulkuploads/error", System.getProperty("java.io.tmpdir")));
        errorDir.mkdirs();
        return new File(errorDir, format("%s.csv", uuid));
    }
}
