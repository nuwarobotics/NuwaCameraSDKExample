package com.nuwarobotics.camerasdkdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.nuwarobotics.lib.visual.model.FaceData;
import com.nuwarobotics.service.camera.common.Constants;
import com.nuwarobotics.service.camera.common.CsDebug;
import com.nuwarobotics.service.camera.sdk.CameraSDK;
import com.nuwarobotics.service.camera.sdk.OutputData;

import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private CameraSDK cameraSDK = new CameraSDK(this);
    private TextView mOutputTextView, mOutputTextView1;
    private Gson mGson;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = (Button) findViewById(R.id.startBtn);
        Button stopBtn = (Button) findViewById(R.id.stopBtn);
        Button bindBtn = (Button) findViewById(R.id.bindBtn);
        Button unbindBtn = (Button) findViewById(R.id.unbindBtn);

        mOutputTextView = (TextView) findViewById(R.id.outputTextView);
        mOutputTextView1 = (TextView) findViewById(R.id.outputTextView1);
        startBtn.setOnClickListener(mOnClickListener);
        stopBtn.setOnClickListener(mOnClickListener);
        bindBtn.setOnClickListener(mOnClickListener);
        unbindBtn.setOnClickListener(mOnClickListener);
        mGson = new Gson();
    }

    private Button.OnClickListener mOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.startBtn: {
                    CsDebug.logD(TAG, "startBtn");
                    Intent intent = new Intent();
                    intent.setClassName(Constants.SERVICE_PACKAGENAME, Constants.SERVICE_CLASSNAME);
                    MainActivity.this.startService(intent);
                    break;
                }
                case R.id.stopBtn: {
                    CsDebug.logD(TAG, "stopBtn");
                    Intent intent = new Intent();
                    intent.setClassName(Constants.SERVICE_PACKAGENAME, Constants.SERVICE_CLASSNAME);
                    MainActivity.this.stopService(intent);
                    break;
                }
                case R.id.bindBtn: {
                    CsDebug.logD(TAG, "bindBtn");
                    //register get which recognition data
                    cameraSDK.register(mCameraSDKCallback,  Constants.FACE_DETECTION
                            |Constants.FACE_RECOGNITION
                            | Constants.GESTURE_RECOGNITION
                            | Constants.OBJ_RECOGNITION
                            | Constants.FACE_TRACK
                            ,this.getClass().getPackage().getName());

                    break;
                }
                case R.id.unbindBtn: {
                    CsDebug.logD(TAG, "unbindBtn");
                    cameraSDK.unregister(this.getClass().getPackage().getName());
                    break;
                }
            }
        }
    };

    private CameraSDK.CameraSDKCallback mCameraSDKCallback = new CameraSDK.CameraSDKCallback() {

        @Override
        public void onConnected(boolean isConnected) {
            CsDebug.logD(TAG, "onConnected:" + isConnected);
        }

        @Override
        public void onOutput(Map<Integer, OutputData> resultMap) {
            CsDebug.logD(TAG, "onOutput:" + resultMap);
            final StringBuilder sb = new StringBuilder();
            for (Integer integer : resultMap.keySet()) {
                OutputData outputData = resultMap.get(integer);
                if (outputData != null) {
                    CsDebug.logD(TAG, "onOutput:" + integer + ": " + outputData.processTime + "\t" + outputData.data);
                    sb.append("" + integer + ": " + outputData.processTime + "\t" + outputData.data + "\n");
                    FaceData returnFace = mGson.fromJson(outputData.data, FaceData.class);
                    Log.d(TAG,"FaceData user_name"+returnFace.name);  //return name come from registered on Robot Family.

                }
            }
            final String info = sb.toString();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOutputTextView.setText(info);
                }
            });
        }

        @Override
        public void onPictureTaken(String path) {
            CsDebug.logV(TAG, "image path=" + path);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        cameraSDK.unregister(this.getPackageName());
        super.onPause();
    }
}
