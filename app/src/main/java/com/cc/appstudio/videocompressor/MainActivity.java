package com.cc.appstudio.videocompressor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialcamera.MaterialCamera;
import com.cc.appstudio.videocompressor.services.VideoCompressService;
import com.cc.appstudio.videocompressor.utilities.FileUtils;

import java.io.File;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final int CAMERA_RQ = 6969;
    private static final int VIDEO_RECORD_RQ = 6959;
    private static final int COMPRESS_FILE_SELECTION_RQ = 6669;
    private static final int PERMISSION_RQ_GALLERY = 80;
    private static final int PERMISSION_RQ_VIDEO = 81;
    private static final int PERMISSION_RQ_CAMERA = 82;
    private File saveDir = null;

    public static final String ROOT_FOLDER_NAME = "VideoCompressor";
    public static final String SUB_FOLDER_NAME = ROOT_FOLDER_NAME
            + File.separator + "Compressed Files";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        saveDir = FileUtils.createFileDir(this, ROOT_FOLDER_NAME);
        if (saveDir.exists()) {
            FileUtils.createFileDir(this, SUB_FOLDER_NAME);
        }

    }

    //region Click event methods
    @OnClick(R.id.btn_create_image)
    public void startCameraLaunch(View view) {
        openInAppCamera();
    }

    @OnClick(R.id.btn_create_video)
    public void startVideoLaunch(View view) {
        startInAppVideoCamera();
    }


    @OnClick(R.id.btn_select_compress_video)
    public void selectVideoFromGallery(View view) {
        openGalleryIntent();
    }

    //endregion

    //region Helper methods for Permission request and callbacks
    @AfterPermissionGranted(PERMISSION_RQ_GALLERY)
    public void openGalleryIntent() {
        String perm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this,
                    "Need external permission " + "for accessing Gallery",
                    PERMISSION_RQ_GALLERY, perm);
        } else {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, COMPRESS_FILE_SELECTION_RQ);
            }
        }
    }

    @AfterPermissionGranted(PERMISSION_RQ_CAMERA)
    public void openInAppCamera() {
        String perm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this,
                    "Need external permission for video",
                    PERMISSION_RQ_CAMERA, perm);
        } else {
            MaterialCamera materialCamera = new MaterialCamera(this)
                    .stillShot()
                    .showPortraitWarning(false)
                    .saveDir(saveDir)
                    .allowRetry(true)
                    .defaultToFrontFacing(false)
                    .allowRetry(true)
                    .autoSubmit(false)
                    .labelConfirm(R.string.select_photo_done);

            materialCamera.start(CAMERA_RQ);
        }
    }

    @AfterPermissionGranted(PERMISSION_RQ_VIDEO)
    public void startInAppVideoCamera() {
        String perm = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (!EasyPermissions.hasPermissions(this, perm)) {
            EasyPermissions.requestPermissions(this,
                    "Need external permission for video",
                    PERMISSION_RQ_VIDEO, perm);
        } else {
            MaterialCamera materialCamera;
            if (isDeviceSamsung()) {
                materialCamera = new MaterialCamera(this)
                        .saveDir(saveDir)
                        .allowRetry(true)
                        .showPortraitWarning(false)
                        .defaultToFrontFacing(false)
                        .allowRetry(true)
                        .autoSubmit(false)
                        // Sets a custom frame rate (FPS) for video recording.
                        .videoFrameRate(30)
                        // Sets a quality profile, manually setting bit rates or frame rates with
                        // other settings will overwrite individual quality profile settings
                        .qualityProfile(MaterialCamera.QUALITY_720P)
                        // Sets a preferred height for the recorded video output.
                        .videoPreferredHeight(720)
                        .videoPreferredAspect(16f / 9f)
                        .forceCamera1()
                        .labelConfirm(R.string.mcam_use_video);
            } else {
                materialCamera = new MaterialCamera(this)
                        .saveDir(saveDir)
                        .allowRetry(true)
                        .showPortraitWarning(false)
                        .defaultToFrontFacing(false)
                        .allowRetry(true)
                        .autoSubmit(false)
                        // Sets a custom frame rate (FPS) for video recording.
                        .videoFrameRate(30)
                        // Sets a quality profile, manually setting bit rates or frame rates with
                        // other settings will overwrite individual quality profile settings
                        .qualityProfile(MaterialCamera.QUALITY_720P)
                        // Sets a preferred height for the recorded video output.
                        .videoPreferredHeight(720)
                        .videoPreferredAspect(16f / 9f)
                        .labelConfirm(R.string.mcam_use_video);
            }
            materialCamera.start(VIDEO_RECORD_RQ);
        }

    }
    //endregion

    //region override methods
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {

        } else if (requestCode == CAMERA_RQ) {
            if (resultCode == RESULT_OK) {
                final File file = new File(data.getData().getPath());

                Toast.makeText(this, formatString(file.getAbsolutePath(),
                        file.length()), Toast.LENGTH_LONG).show();
                forceUpdateMediaScanner(this, file.getAbsolutePath());
            } else if (data != null) {
                Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                if (e != null) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == VIDEO_RECORD_RQ) {
            if (resultCode == RESULT_OK) {
                final File file = new File(data.getData().getPath());
                Toast.makeText(this, formatString(file.getAbsolutePath(),
                        file.length()), Toast.LENGTH_LONG).show();
                forceUpdateMediaScanner(this, file.getAbsolutePath());
                startCompressServiceForVideo(file.getAbsolutePath());
            } else if (data != null) {
                Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                if (e != null) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == COMPRESS_FILE_SELECTION_RQ) {
            if (resultCode == RESULT_OK) {
                final File file = FileUtils.getFileFromUri(this, data.getData());
                startCompressServiceForVideo(file.getAbsolutePath());
            }
        }

    }

    public static void forceUpdateMediaScanner(Context context, String filePathForScan) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://" + filePathForScan)));
        } else {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + filePathForScan)));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == PERMISSION_RQ_CAMERA) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                new AppSettingsDialog.Builder(this).build().show();
            }
        } else if (requestCode == PERMISSION_RQ_VIDEO) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                new AppSettingsDialog.Builder(this).build().show();
            }
        } else if (requestCode == PERMISSION_RQ_GALLERY) {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                new AppSettingsDialog.Builder(this).build().show();
            }
        }
    }

    //endregion

    //region General Helper methods

    private boolean isDeviceSamsung() {
        String deviceMan = android.os.Build.MANUFACTURER;
        return deviceMan.equalsIgnoreCase("samsung");
    }

    private void startCompressServiceForVideo(String filePath) {
        Bundle bundle = new Bundle();
        bundle.putString("filePath", filePath);
        Intent msgIntent = new Intent(this, VideoCompressService.class);
        msgIntent.putExtras(bundle);
        startService(msgIntent);
    }

    private void openVideoGalleryIntent() {

    }

    private String formatString(String filePath, long fileSizeLength) {
        return String.format("Saved to: %s, size: %s", filePath,
                FileUtils.formatFileSize(this, fileSizeLength));
    }

    //endregion
}
