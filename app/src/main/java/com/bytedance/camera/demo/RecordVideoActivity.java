package com.bytedance.camera.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

public class RecordVideoActivity extends AppCompatActivity {

    private VideoView videoView;
    private final String TAG = "RECORDACTIVITY";
    private static final int REQUEST_VIDEO_CAPTURE = 1;

    private static final int REQUEST_EXTERNAL_CAMERA = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_video);

        videoView = findViewById(R.id.img);

        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            if ((ContextCompat.checkSelfPermission(RecordVideoActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED )||
                (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        || (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_CAMERA);

                //todo 在这里申请相机、存储的权限
            } else {
                Intent vedioIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (vedioIntent.resolveActivity(getPackageManager()) != null) {
                    Log.e(TAG, "BEFORE THE RECORD");
                    startActivityForResult(vedioIntent, REQUEST_VIDEO_CAPTURE);
                }
                //todo 打开相机拍摄
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            boolean stop = false;
            @Override
            public void onClick(View view) {
                Log.e(TAG, "onClick: ");
                if (stop == true) {
                    stop = false;
                    videoView.start();
                } else {
                    stop = true;
                    videoView.pause();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.e(TAG, "onActivityResult: ");
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            //todo 播放刚才录制的视频
            Uri vediouri = intent.getData();
            videoView.setVideoURI(vediouri);
            videoView.start();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_CAMERA: {
                //todo 判断权限是否已经授予
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "授权已经被授予", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
}
