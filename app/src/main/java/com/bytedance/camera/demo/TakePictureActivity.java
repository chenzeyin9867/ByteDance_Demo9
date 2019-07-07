package com.bytedance.camera.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import com.bytedance.camera.demo.utils.Utils;

import java.io.File;



public class TakePictureActivity extends AppCompatActivity {

    private ImageView imageView;
    private File imageFile;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final int REQUEST_EXTERNAL_STORAGE = 101;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);
        imageView = findViewById(R.id.img);
        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(TakePictureActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(TakePictureActivity.this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //todo 在这里申请相机、存储的权限
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA},REQUEST_EXTERNAL_STORAGE);

            } else {
                takePicture();
            }
        });

    }

    private void takePicture() {
        //todo 打开相机
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageFile = Utils.getOutputMediaFile(Utils.MEDIA_TYPE_IMAGE);
        if(imageFile !=null){
            Uri fileUri = FileProvider.getUriForFile(this,"com.bytedance.camera.demo",imageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
            startActivityForResult(intent,REQUEST_IMAGE_CAPTURE);
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setPic();
        }
    }

    private void setPic() {
        int targetH = imageView.getHeight();
        int targetW = imageView.getWidth();
        BitmapFactory.Options bmoptions = new BitmapFactory.Options();
        bmoptions.inJustDecodeBounds= true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(),bmoptions);
        int ph = bmoptions.outHeight;
        int pw = bmoptions.outWidth;
        int scale = Math.min(ph/targetH,pw/targetW);
        bmoptions.inJustDecodeBounds = false;
        bmoptions.inSampleSize = scale;
        bmoptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(),bmoptions);
        imageView.setImageBitmap(bitmap);

        //todo 根据imageView裁剪
        //todo 根据缩放比例读取文件，生成Bitmap

        //todo 如果存在预览方向改变，进行图片旋转

        //todo 如果存在预览方向改变，进行图片旋转
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                //todo 判断权限是否已经授予
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"授权已经被授予",Toast.LENGTH_SHORT).show();
                }

                break;
            }
        }
    }

    public static Bitmap rotateImage(Bitmap bitmap,String path) throws  Exception{
        ExifInterface srcExif = new ExifInterface(path);
        Matrix matrix = new Matrix();
        int angle = 0;
        int orientation = srcExif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
        switch (orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                angle = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                angle = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                angle = 270;
                break;
             default:
                 break;

        }
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }


}
