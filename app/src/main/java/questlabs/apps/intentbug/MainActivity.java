package questlabs.apps.intentbug;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private Button mButton;
    private Button permissionButton;
    private ImageView mImageview;
    private final int PICK_IMAGE = 1191;
    private final int CROP_IMAGE = 1001;
    private final int PERMISSION_CODE = 1511;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button) findViewById(R.id.button);
        permissionButton = (Button) findViewById(R.id.permissionButton);
        mImageview = (ImageView) findViewById(R.id.imageView);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGalleryIntent();
            }
        });

        permissionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    void checkPermission(){
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission is granted");
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }
    }

    /**
     * Intent to open Gallery on Android. On Android > M opens the Documents chooser.
     */
    void openGalleryIntent(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }

    /**
     * Intent to avoid the Documents chooser and directly rely on the gallery apps.
     */
    public void pickImageAndCrop() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("outputX", 256);
        intent.putExtra("outputY", 256);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CROP_IMAGE);
    }

    /**
     * Convert the URI to file:// path
     * @param imageUri uri to be converted
     * @return file:// path of the image
     */
    String getImagePathFromURI(Uri imageUri){
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        String picturePath = null;
        Cursor cursor = getContentResolver().query(imageUri,
                filePathColumn, null, null, null);
        if(cursor!=null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();
        }
        return picturePath;
    }

    /**
     * Whaat? Why are you getting Uri from Uri? Are you insane?
     * Maybe.? Doing this because developers at Google like to make our lives difficult. -_-
     * (Actually, when using the Photos app, Google does give the proper URI.
     * Realized this after a couple of hours.
     * But Photos app is still a dick.)
     * @param uri the uri from Photos app content://com.google.android.apps.photos.contentprovider/0/1/content%3A%2F%2Fmedia%2Fexternal%2Fimages%2Fmedia%2F6425/ORIGINAL/NONE/238140102
     * @return uri based on the exact location - this doesn't work anymore. Use FileProvider to get something like content://questlabs.apps.intentbug.provider/external_files/DCIM/Camera/IMG_20170406_183447.jpg
     */
    Uri getUriForContent(Uri uri){
        return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", new File(getImagePathFromURI(uri)));
    }

    /**
     * Intent to open image in default crop provider.
     * @param imageUri uri of the image to be opened
     */
    void sendImageToCrop(Uri imageUri){
        try {
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            cropIntent.setDataAndType(imageUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 128);
            cropIntent.putExtra("outputY", 128);
            cropIntent.putExtra("return-data", true);
            startActivityForResult(cropIntent, CROP_IMAGE);
        }
        catch (ActivityNotFoundException anfe) {
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Opens the image in a full fledged image editor.
     * @param imageUri uri of the image
     */
    void editImage(Uri imageUri){
        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setDataAndType(imageUri, "image/*");
        editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(Intent.createChooser(editIntent, null),CROP_IMAGE);
    }

    /**
     * Last resort. Use a 3rd party library to do the thang!
     * https://github.com/ArthurHub/Android-Image-Cropper
     * @param imageUri uri of the image
     */
    void cropImage(Uri imageUri){
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            openGalleryIntent();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case PICK_IMAGE:
                    if(data!=null){
                        Uri selectedImage = data.getData();
                        sendImageToCrop(selectedImage);
                    }else{
                        Toast.makeText(this, "Unable to load image. Try again", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CROP_IMAGE:
                    if (data != null) {
                        Bundle extras = data.getExtras();
                        if(extras!=null) {
                            Bitmap bitmap = extras.getParcelable("data");
                            mImageview.setImageBitmap(bitmap);
                        }else{
                            sendImageToCrop(getUriForContent(data.getData()));
                        }
                    } else {
                        Toast.makeText(this, "Unable to load image. Try again", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }
    }
}
