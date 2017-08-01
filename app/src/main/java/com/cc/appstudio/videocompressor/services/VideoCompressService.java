package com.cc.appstudio.videocompressor.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.cc.appstudio.videocompressor.engine.VideoResolutionChanger;

import java.io.File;
import java.text.DecimalFormat;

public class VideoCompressService extends IntentService {

    private String filePathForCompress;
    private File filePath;
    String TAG = "IntentServiceForVideoCompress";


    public VideoCompressService() {
        super("VideoCompressService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                filePathForCompress = bundle.getString("filePath");
                filePath = new File(filePathForCompress);
                startCompressVideoProcess(filePath);
            }
        }
    }


    private void startCompressVideoProcess(File filePAth) {
        try {
            String pathToReEncodedFile = new VideoResolutionChanger().changeResolution(filePAth);
            final File compressedFilePath = new File(pathToReEncodedFile);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            String.format("Saved compressed to: %s, size: %s",
                                    compressedFilePath.getAbsolutePath(),
                                    fileSize(compressedFilePath)),
                            Toast.LENGTH_LONG)
                            .show();
                }
            });

        } catch (Exception ex) {
            showMessage(ex.getMessage());
            Log.d(TAG, ex.getMessage(), ex);
        } catch (Throwable throwable) {
            showMessage(throwable.getMessage());
            Log.d(TAG, throwable.getMessage(), throwable);

        }
    }

    private String fileSize(File file) {
        return readableFileSize(file.length());
    }


    private String readableFileSize(long size) {
        if (size <= 0) return size + " B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups))
                + " "
                + units[digitGroups];
    }

    private void showMessage(final String errorMessage) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
