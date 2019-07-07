package com.bytedance.camera.demo;

import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.bytedance.camera.demo.utils.Utils.MEDIA_TYPE_IMAGE;
import static com.bytedance.camera.demo.utils.Utils.MEDIA_TYPE_VIDEO;
import static com.bytedance.camera.demo.utils.Utils.getOutputMediaFile;

public class CustomCameraActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    private Camera mCamera;
    private final String TAG = "CUSTOM CAMERA";
    private int CAMERA_TYPE = Camera.CameraInfo.CAMERA_FACING_BACK;

    private boolean isRecording = false;
    private MediaRecorder mMediaRecorder;
    private int rotationDegree = 0;
    private boolean front = false;
    private String currentMp4path;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_custom_camera);
        mSurfaceView = findViewById(R.id.img);
        mCamera = getCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
//        Camera.Parameters param = mCamera.getParameters();
//        Camera.Size size = getOptimalPreviewSize(param.getSupportedPreviewSizes(),mSurfaceView.getWidth(),mSurfaceView.getHeight());
//        param.setPreviewSize(size.width,size.height);
//        mCamera.setParameters(param);

        //todo 给SurfaceHolder添加Callback
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mCamera.setPreviewDisplay(surfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mCamera.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;

            }
        });


        findViewById(R.id.btn_picture).setOnClickListener(v -> {
            //todo 拍一张照片
            mCamera.takePicture(null, null, mPicture);

        });

        findViewById(R.id.btn_record).setOnClickListener(v -> {
            //todo 录制，第一次点击是start，第二次点击是stop
            if (isRecording) {
                //todo 停止录制
                isRecording = false;
                releaseMediaRecorder();
                String filePath = currentMp4path;
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
                Toast.makeText(this, "保存成功：" + filePath, Toast.LENGTH_LONG).show();

            } else {
                //todo 录制
                isRecording = true;
                prepareVideoRecorder();
                startPreview(mSurfaceView.getHolder());


                try {
                    mMediaRecorder.prepare();
                    mMediaRecorder.start();
                } catch (Exception e) {
                    releaseMediaRecorder();

                }


            }
        });

        findViewById(R.id.btn_facing).setOnClickListener(v -> {
            //todo 切换前后摄像头
            releaseCameraAndPreview();
            if (front) {
                front = false;
                mCamera = getCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                front = true;
                mCamera = getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }
            try {
                Camera.Parameters param1 = mCamera.getParameters();
                Camera.Size size1 = getOptimalPreviewSize(param1.getSupportedPreviewSizes(),mSurfaceView.getWidth(),mSurfaceView.getHeight());
                param1.setPreviewSize(size1.width,size1.height);
                mCamera.setParameters(param1);
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        findViewById(R.id.btn_zoom).setOnClickListener(v -> {
            //todo 调焦，需要判断手机是否支持
            System.out.println(mCamera.getParameters().isZoomSupported());
            Log.e("ZOOM", mCamera.getParameters().isZoomSupported() ? "true" : "false");
            if (mCamera != null && mCamera.getParameters().isZoomSupported()) {

                Camera.Parameters parameters = mCamera.getParameters();

                int zoom = parameters.getZoom();
                if ((zoom = (zoom + 1) * 2) >= parameters.getMaxZoom()) {
                    zoom = 0;
                }
                parameters.setZoom(zoom);
                mCamera.setParameters(parameters);

            } else {
                Toast.makeText(this, "该机型不支持对焦", Toast.LENGTH_LONG).show();
            }

        });
    }

    public Camera getCamera(int position) {
        CAMERA_TYPE = position;
        if (mCamera != null) {
            releaseCameraAndPreview();
        }
        mCamera = Camera.open(position);
        rotationDegree = getCameraDisplayOrientation(position);
        mCamera.setDisplayOrientation(rotationDegree);

        if (mCamera != null && mCamera.getParameters().isZoomSupported()) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    if (b) {
                        Toast.makeText(CustomCameraActivity.this, ">>>>>>>>success", Toast.LENGTH_LONG).show();
                    } else {
                        camera.autoFocus(this);//如果失败，自动聚焦

                    }
                }
            });
        } else {
            Toast.makeText(this, "该机型不支持对焦", Toast.LENGTH_LONG).show();
        }
        //todo 摄像头添加属性，例是否自动对焦，设置旋转方向等

        return mCamera;
    }


    private static final int DEGREE_90 = 90;
    private static final int DEGREE_180 = 180;
    private static final int DEGREE_270 = 270;
    private static final int DEGREE_360 = 360;

    private int getCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = DEGREE_90;
                break;
            case Surface.ROTATION_180:
                degrees = DEGREE_180;
                break;
            case Surface.ROTATION_270:
                degrees = DEGREE_270;
                break;
            default:
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % DEGREE_360;
            result = (DEGREE_360 - result) % DEGREE_360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + DEGREE_360) % DEGREE_360;
        }
        return result;
    }


    private void releaseCameraAndPreview() {
        //todo 释放camera资源
        mCamera.stopPreview();
        mCamera.release();
        // mCamera.unlock();
        mCamera = null;
    }

    Camera.Size size;

    private void startPreview(SurfaceHolder holder) {
        //todo 开始预览
        // set the preview
        mMediaRecorder.setPreviewDisplay(holder.getSurface());
        mMediaRecorder.setOrientationHint(rotationDegree);
    }




    private boolean prepareVideoRecorder() {
        //todo 准备MediaRecorder
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);


        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // set the configure
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));

        // set out put file
        currentMp4path = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
        mMediaRecorder.setOutputFile(currentMp4path);
        Log.e("FUCK", "prepareVideoRecorder: "+Environment.getExternalStorageDirectory().getPath());
        return true;
    }


    private void releaseMediaRecorder() {
        //todo 释放MediaRecorder
        mMediaRecorder.stop();
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCamera.lock();
    }


    private Camera.PictureCallback mPicture = (data, camera) -> {
        File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        Log.e(TAG,pictureFile.getPath() );
        if (pictureFile == null) {
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            Log.d("mPicture", "Error accessing file: " + e.getMessage());
        }
        String filePath = pictureFile.getAbsolutePath();
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + filePath)));
        Toast.makeText(this, "保存成功：" + filePath, Toast.LENGTH_LONG).show();

        mCamera.startPreview();

    };


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(w, h);

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}
