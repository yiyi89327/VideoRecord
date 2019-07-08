package com.xhh.videocapture;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.Arrays;

public class PermissionActivity extends Activity {

    private static final String TAG = "PermissionActivity";
    private static final int INIT_REQUEST_CODE = 100;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    private static final int RECORD_AUDIO_REQUEST_CODE = 103;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        applyNextPermission(INIT_REQUEST_CODE, null);

//        ActivityCompat.requestPermissions(this,
//                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.CAMERA,
//                        Manifest.permission.RECORD_AUDIO},
//                INIT_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        applyNextPermission(requestCode,grantResults);
    }

    private void applyNextPermission(int requestCode, int[] grantResults) {
        Log.i(TAG, "applyNextPermission: requestCode = " + requestCode + ", grantResults = " + Arrays.toString(grantResults));
        switch (requestCode) {
            case INIT_REQUEST_CODE:
                applySroragePermission();
                break;
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                if (null == grantResults || (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    applyCameraPermission();
                } else {
                    showRejectedDialog();
                }
                break;
            case CAMERA_REQUEST_CODE:
                if (null == grantResults || (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    applyRecordPermission();
                } else {
                    showRejectedDialog();
                }
                break;
            case RECORD_AUDIO_REQUEST_CODE:
                if (null == grantResults || (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    moveToMainActivity();
                } else {
                    showRejectedDialog();
                }
                break;
        }
    }

    private void showRejectedDialog() {
        new AlertDialog.Builder(PermissionActivity.this)
                .setTitle("权限被拒")
                .setMessage(R.string.permission_rationale)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        PermissionActivity.this.finish();
                    }
                })
                .show();
    }

    /**
     * 申请WRITE_EXTERNAL_STORAGE权限
     */
    private void applySroragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            applyNextPermission(WRITE_EXTERNAL_STORAGE_REQUEST_CODE, null);
        }
    }

    /**
     * 申请CAMERA权限
     */
    private void applyCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        } else {
            applyNextPermission(CAMERA_REQUEST_CODE, null);
        }
    }

    /**
     * 申请RECORD_AUDIO权限
     */
    private void applyRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_REQUEST_CODE);
        } else {
            applyNextPermission(RECORD_AUDIO_REQUEST_CODE, null);
        }
    }

    private void moveToMainActivity() {
        Log.i(TAG, "权限获取完毕，跳转至Main页");
        startActivity(new Intent(PermissionActivity.this, MainActivity.class));
        finish();
    }
}
