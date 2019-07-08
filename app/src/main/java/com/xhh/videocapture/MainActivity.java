package com.xhh.videocapture;

import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dinuscxj.progressbar.CircleProgressBar;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
        , SurfaceHolder.Callback, MediaRecorder.OnErrorListener{

    private static final String TAG = "MainActivity";

    //存放照片的文件夹
    public final static String  BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/video/";
    public static final int CAMERA_FACING_BACK = 0;
    public static final int CAMERA_FACING_FRONT = 1;

    private SurfaceView mSurfaceView;
    private LinearLayout layToolLl;
    private ImageView exitBtn;
    private ImageView lightBtn;
    private ImageView switchBtn;
    private ImageView tagStart;
    private CircleProgressBar mProgressBar;
    private ImageView startBtn;

    private AnimationDrawable anim;
    private MediaRecorder mMediaRecorder;// 录制视频的类
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Timer mTimer;// 计时器
    private TimerTask timerTask;
    private boolean isOpenCamera = true;// 是否一开始就打开摄像头
    private int mRecordMaxTime = 1000;// 一次拍摄最长时间 10秒
    private int mTimeCount;// 时间计数
    private File mRecordFile = null; // 文件
    private File mRecordDir; // 文件夹

    private boolean isStarting = false;
    private List<int[]> mFpsRange;
    private Camera.Size optimalSize;
    private Camera.Parameters parameters;
    private boolean isFlashLightOn = false;
    private int cameraPosition = CAMERA_FACING_FRONT;
    //视频存储的目录
    private String dirname;

    private OnRecordFinishListener mOnRecordFinishListener; // 录制完成回调接口
    private OnRecordFinishListener recordFinishListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 去掉标题栏，并设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar supportActionBar = getSupportActionBar();
        if (null != supportActionBar) supportActionBar.hide();
        initData();
        initView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stop();
    }

    @Override
    public void onBackPressed() {
        stop();
        finish();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surfaceView);
        layToolLl = findViewById(R.id.lay_tool);
        exitBtn = findViewById(R.id.exitBtn);
        switchBtn = findViewById(R.id.switchCamera);
        lightBtn = findViewById(R.id.lightBtn);
        tagStart = findViewById(R.id.tag_start);
        mProgressBar = findViewById(R.id.progress);
        startBtn = findViewById(R.id.startBtn);

        lightBtn.setOnClickListener(this);
        exitBtn.setOnClickListener(this);
        switchBtn.setOnClickListener(this);

        anim = (AnimationDrawable) tagStart.getDrawable();
        anim.setOneShot(false); // 设置是否重复播放
        mSurfaceHolder = mSurfaceView.getHolder();// 取得holder
        mSurfaceHolder.addCallback(this); // holder加入回调接口
        mSurfaceHolder.setKeepScreenOn(true);

        startBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN :
                        if(isStarting){
                            stopRecord();
                        }else {
                            startRecord(recordFinishListener);
                        }
                        Log.i(TAG, "ACTION: DOWN");
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG, "ACTION: UP");
                        if(mTimeCount < 10){
                            Toast.makeText(MainActivity.this, "不能少于1秒！", Toast.LENGTH_SHORT).show();
                            deleteFile(mRecordDir);
                            stopRecord();
                        } else {
                            stopRecord();
                            if (mOnRecordFinishListener != null){
                                mOnRecordFinishListener.onRecordFinish();
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void initData() {
        recordFinishListener = new OnRecordFinishListener() {
            @Override
            public void onRecordFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "拍摄完毕", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };
    }

    /**
     * 开关闪光灯
     */
    private void flashLightToggle(){
        try {
            if(isFlashLightOn){
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                isFlashLightOn = false;
            }else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                isFlashLightOn = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换摄像头
     */
    private void switchCamera(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();//得到摄像头的个数
        Log.i(TAG, "cameraCount = " + cameraCount);
        //得到每一个摄像头的信息
//        for(int i = 0; i < cameraCount; i++ ) {
//            Camera.getCameraInfo(i, cameraInfo);
//        }
        if(cameraPosition == CAMERA_FACING_FRONT) {
            //现在是前置，变更为后置
            Camera.getCameraInfo(CAMERA_FACING_FRONT, cameraInfo);
            Log.i(TAG, "前置-->后置  cameraInfo.facing = " + cameraInfo.facing);
            if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_FRONT) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                mCamera.stopPreview();//停掉原来摄像头的预览
                mCamera.release();//释放资源
                mCamera = null;//取消原来摄像头
                mCamera = Camera.open(CAMERA_FACING_BACK);//打开当前选中的摄像头
                try {
                    mCamera.setDisplayOrientation(90);
                    mCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                mCamera.setParameters(parameters);// 设置相机参数
                mCamera.startPreview();//开始预览
                cameraPosition = CAMERA_FACING_BACK;
            }
        } else {
            //现在是后置， 变更为前置
            Camera.getCameraInfo(CAMERA_FACING_BACK, cameraInfo);
            Log.i(TAG, "后置-->前置 cameraInfo.facing = " + cameraInfo.facing);
            if(cameraInfo.facing  == Camera.CameraInfo.CAMERA_FACING_BACK) {//代表摄像头的方位，CAMERA_FACING_FRONT前置      CAMERA_FACING_BACK后置
                mCamera.stopPreview();//停掉原来摄像头的预览
                mCamera.release();//释放资源
                mCamera = null;//取消原来摄像头
                mCamera = Camera.open(CAMERA_FACING_FRONT);//打开当前选中的摄像头
                try {
                    mCamera.setDisplayOrientation(90);
                    mCamera.setPreviewDisplay(mSurfaceHolder);//通过surfaceview显示取景画面
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                mCamera.setParameters(parameters);// 设置相机参数
                mCamera.startPreview();//开始预览
                cameraPosition = CAMERA_FACING_FRONT;
            }
        }
    }

    /**
     * 开始录制视频
     */
    public void startRecord(final OnRecordFinishListener onRecordFinishListener) {
        this.mOnRecordFinishListener = onRecordFinishListener;
        isStarting = true;
        layToolLl.setVisibility(View.INVISIBLE);
        tagStart.setVisibility(View.VISIBLE);
        anim.start();
        createRecordDir();
        try {
            initRecord();
            mTimeCount = 0;// 时间计数器重新赋值
            mTimer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    mTimeCount++;
                    mProgressBar.setProgress(mTimeCount/10);
                    if (mTimeCount == mRecordMaxTime) {// 达到指定时间，停止拍摄
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stop();
                            }
                        });
                        if (mOnRecordFinishListener != null){
                            mOnRecordFinishListener.onRecordFinish();
                        }

                    }
                }
            };
            mTimer.schedule(timerTask, 0, 100);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        mProgressBar.setProgress(0);
        isStarting = false;
        tagStart.setVisibility(View.GONE);
        anim.stop();
        layToolLl.setVisibility(View.VISIBLE);
        if(timerTask != null)
            timerTask.cancel();
        if (mTimer != null)
            mTimer.cancel();
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (RuntimeException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 释放资源
     */
    private void releaseRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setPreviewDisplay(null);
            mMediaRecorder.setOnErrorListener(null);
            try {
                mMediaRecorder.release();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mMediaRecorder = null;
    }

    /**
     * 释放摄像头资源
     */
    private void freeCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 录制前，初始化
     */
    private void initRecord() {
        try {
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
            }
            if (mCamera != null) {
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);
            }
            mMediaRecorder.setOnErrorListener(this);
            // 音频源
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 视频源
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            // 输出方向
            if (cameraPosition == CAMERA_FACING_FRONT) {
                mMediaRecorder.setOrientationHint(270);
            } else {
                mMediaRecorder.setOrientationHint(90);
            }
            // Use the same size for recording profile.
            CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mProfile.videoFrameWidth = optimalSize.width;
            mProfile.videoFrameHeight = optimalSize.height;
            mMediaRecorder.setProfile(mProfile);
            //该设置是为了抽取视频的某些帧，真正录视频的时候，不要设置该参数
//            mMediaRecorder.setCaptureRate(mFpsRange.get(0)[0]);//获取最小的每一秒录制的帧数
            mMediaRecorder.setOutputFile(mRecordFile.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            releaseRecord();
        }
    }

    /**
     * 停止拍摄
     */
    public void stop() {
        stopRecord();
        releaseRecord();
        freeCameraResource();
    }


    /**
     * 创建目录与文件
     */
    private void createRecordDir() {
        dirname = String.valueOf(System.currentTimeMillis()) +  String.valueOf( new Random().nextInt(1000));
        mRecordDir = new File(BASE_PATH + dirname);
        if (!mRecordDir.exists()) {
            mRecordDir.mkdirs();
        }
        // 创建文件
        try {
            mRecordFile = new File(mRecordDir.getAbsolutePath() + "/" + Utils.getDateNumber() +".mp4");
            Log.d(TAG, "Path:" + mRecordFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除文件
     * @param file
     */
    public static void deleteFile(File file) {
        if (null == file)
            return;
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
        } else if (file.isDirectory()) {
            File files[] = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }
        }
        file.delete();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.exitBtn:
                stop();
                finish();
                break;
            case R.id.lightBtn:
                flashLightToggle();
                break;
            case R.id.switchCamera:
                switchCamera();
                break;
        }
    }


    // SurfaceView start
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mSurfaceHolder = holder;
        if (mCamera != null) {
            freeCameraResource();
        }
        try {
            mCamera = Camera.open(cameraPosition);
            if (mCamera == null) {
                Log.e(TAG, "Camera为空");
                return;
            }
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            parameters = mCamera.getParameters();// 获得相机参数

            List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
            optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                    mSupportedPreviewSizes, height, width);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height); // 设置预览图像大小
            parameters.setPictureSize(1920, 1080); // 设置预览图像大小
            parameters.set("orientation", "portrait");
            List<String> focusModes = parameters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            mFpsRange =  parameters.getSupportedPreviewFpsRange();
            mCamera.setParameters(parameters);// 设置相机参数
            mCamera.startPreview();// 开始预览
        } catch (Exception io){
            io.printStackTrace();
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        freeCameraResource();
    }

    // SurfaceView end

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 录制完成回调接口
     */
    public interface OnRecordFinishListener {
        void onRecordFinish();
    }



}

