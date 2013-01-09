package com.uploadcare.upload;

import com.uploadcare.api.Client;
import com.uploadcare.api.File;
import com.uploadcare.api.RequestHelper;
import com.uploadcare.data.UploadFromUrlData;
import com.uploadcare.data.UploadFromUrlStatusData;
import com.uploadcare.urls.Urls;
import org.apache.http.client.methods.HttpGet;

import java.net.URI;

public class UrlUploader implements Uploader {

    private final Client client;
    private final RequestHelper requestHelper;
    private final String sourceUrl;

    public UrlUploader(Client client, String sourceUrl) {
        this.client = client;
        this.requestHelper = new RequestHelper(client);
        this.sourceUrl = sourceUrl;
    }

    @Override
    public File upload() throws UploadFailureException {
        return upload(500);
    }

    public File upload(int pollingInterval) throws UploadFailureException {
        URI uploadUrl = Urls.uploadFromUrl(sourceUrl, client.getPublicKey());
        String token = requestHelper.executeQuery(new HttpGet(uploadUrl), false, UploadFromUrlData.class).token;
        URI statusUrl = Urls.uploadFromUrlStatus(token);
        while (true) {
            sleep(pollingInterval);
            HttpGet request = new HttpGet(statusUrl);
            UploadFromUrlStatusData data = requestHelper.executeQuery(request, false, UploadFromUrlStatusData.class);
            if (data.status.equals("success")) {
                return client.getFile(data.fileId);
            } else if (data.status.equals("error") || data.status.equals("failed")) {
                throw new UploadFailureException();
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}