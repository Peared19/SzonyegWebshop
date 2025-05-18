package com.example.webshopfinal;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagePicker {
    private Activity activity;
    private String currentPhotoPath;
    private Uri imageUri;

    public static final int CAMERA_PERMISSION_CODE = 1001;
    public static final int GALLERY_PERMISSION_CODE = 1002;
    public static final int CAMERA_REQUEST_CODE = 2001;
    public static final int GALLERY_REQUEST_CODE = 2002;

    public ImagePicker(Activity activity) {
        this.activity = activity;
    }

    public void checkCameraPermission(Runnable onPermissionGranted) {
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            onPermissionGranted.run();
        }
    }

    public void checkGalleryPermission(Runnable onPermissionGranted) {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(
                activity,
                permission
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{permission},
                    GALLERY_PERMISSION_CODE
            );
        } else {
            onPermissionGranted.run();
        }
    }

    public void openCamera(ActivityResultLauncher<Intent> launcher) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (Exception ex) {
            
        }

        if (photoFile != null) {
            imageUri = FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    photoFile
            );
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            launcher.launch(takePictureIntent);
        }
    }

    public void openGallery(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        launcher.launch(intent);
    }

    private File createImageFile() throws Exception {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = activity.getExternalFilesDir("Pictures");
        File image = File.createTempFile(
                "JPEG_" + timeStamp + "_",
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public String getCurrentPhotoPath() {
        return currentPhotoPath;
    }
} 