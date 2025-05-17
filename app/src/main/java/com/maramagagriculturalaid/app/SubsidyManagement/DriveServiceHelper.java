package com.maramagagriculturalaid.app.SubsidyManagement;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Helper class for working with Google Drive API
 */
public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    public DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    public Task<String> createFile(String name, String mimeType, java.io.File fileContent) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setMimeType(mimeType)
                    .setName(name);

            FileContent mediaContent = new FileContent(mimeType, fileContent);
            File googleFile = mDriveService.files().create(metadata, mediaContent)
                    .setFields("id")
                    .execute();

            return googleFile.getId();
        });
    }

    /**
     * Returns a list of all files in the user's My Drive folder.
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, () ->
                mDriveService.files().list()
                        .setSpaces("drive")
                        .setFields("files(id, name, mimeType, thumbnailLink)")
                        .execute());
    }

    /**
     * Returns a list of all image files in the user's My Drive folder.
     */
    public Task<FileList> queryImages() {
        return Tasks.call(mExecutor, () ->
                mDriveService.files().list()
                        .setSpaces("drive")
                        .setQ("mimeType contains 'image/'")
                        .setFields("files(id, name, mimeType, thumbnailLink)")
                        .execute());
    }

    /**
     * Downloads a file from Drive.
     */
    public Task<java.io.InputStream> downloadFile(String fileId) {
        return Tasks.call(mExecutor, () ->
                mDriveService.files().get(fileId)
                        .executeMediaAsInputStream());
    }
}
