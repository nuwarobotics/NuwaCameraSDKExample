package com.nuwarobotics.camerasdkdemo;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.nuwarobotics.lib.visual.model.FaceData;
import com.nuwarobotics.lib.visual.model.Recognition;
import com.nuwarobotics.service.camera.common.Constants;
import com.nuwarobotics.service.camera.common.CsDebug;
import com.nuwarobotics.service.camera.sdk.CameraSDK;
import com.nuwarobotics.service.camera.sdk.OutputData;
import com.nuwarobotics.utils.Debug;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private CameraSDK cameraSDK = new CameraSDK(this);
    private TextView mOutputTextView;
    private Gson mGson;

    private class FRData {
        public static final String UNKNOWN_NAME = "other";
        public int idx;
        public String conf = null;
        public String mask = null; //does user wear mask (support on 1.4620.100JP.1 later version)
        public String name = null; //user name  (if stranger, this value will be "null" or "@#$")
        public Rect rect = null; //face rect
        public String age;
        public String gender;
        public long faceid;

        public FRData() {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button bindBtn = (Button) findViewById(R.id.bindBtn);
        Button unbindBtn = (Button) findViewById(R.id.unbindBtn);

        mOutputTextView = (TextView) findViewById(R.id.outputTextView);
        bindBtn.setOnClickListener(mOnClickListener);
        unbindBtn.setOnClickListener(mOnClickListener);

        Intent intent = new Intent();
        intent.setClassName(Constants.SERVICE_PACKAGENAME, Constants.SERVICE_CLASSNAME);
        MainActivity.this.startService(intent);

        mGson = new Gson();
    }

    private Button.OnClickListener mOnClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bindBtn: {
                    CsDebug.logD(TAG, "bindBtn");
                    //register get which recognition data
                    cameraSDK.register(mCameraSDKCallback,  Constants.FACE_DETECTION
                            |Constants.FACE_RECOGNITION
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
            for (Integer type : resultMap.keySet()) {
                OutputData outputData = resultMap.get(type);
                if (outputData != null && outputData.data != null && !"null".equals(outputData.data)) {
                    CsDebug.logD(TAG, "onOutput:" + type + ": " + outputData.processTime + "\t" + outputData.data);
                    sb.append("" + type + ": " + outputData.processTime + "\t" + outputData.data + "\n");
                    switch(type){
                        case Constants.FACE_DETECTION :{
                            //It allow find all face on preview screen, and get all rect.  (but no mask information)
                            ObjectMapper sMapper = new ObjectMapper();
                            List<Rect> list = null;
                            try {
                                list = sMapper.readValue(outputData.data, new TypeReference<List<Rect>>() {
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (list != null && !list.isEmpty()) {
                                for (Rect rect : list) {
                                    if(rect != null)
                                        Log.d(TAG,"Got face rect:("+rect.bottom+","+rect.left+","+rect.right+","+rect.top+")");
                                }
                            }
                            break;
                        }
                        case Constants.FACE_RECOGNITION :{
                            //It allow recognition face in center of screen.
                            //We can get name、face rect、wear mask information、age、etc.....
                            //Example of get face data
                            // "@#$" is a special unknown result, we need to ignore it
                            FRData returnFace = mGson.fromJson(outputData.data, FRData.class);
                            if(returnFace != null && !"@#$".equals(returnFace.name) && !"null".equals(returnFace.name)){
                                Log.d(TAG,"Find FaceData user_name:"+returnFace.name );  //return name come from registered on Robot Family.
                                Log.d(TAG,"user face rect : "+returnFace.rect.toString());
                                Log.d(TAG,"user wear mask : "+returnFace.mask);
                                Log.d(TAG,"user age : "+returnFace.age);
                            }else{
                                if(returnFace != null)
                                    Log.d(TAG,"Unknown stranger "+returnFace.name);
                            }
                            break;
                        }
                        case Constants.FACE_TRACK :
                            break;
                        case Constants.OBJ_RECOGNITION : {
                            float mConfidence = 0.5f;//standard of confidence
                            ObjectMapper sMapper = new ObjectMapper();

                            List<Recognition> list = null;
                            try {
                                list = sMapper.readValue(outputData.data, new TypeReference<List<Recognition>>() {
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Debug.logD(TAG, "onOutput.list = " + list);
                            if (list != null && !list.isEmpty()) {
                                String title = null;
                                double confidence = 0;
                                //Find best confidence result.
                                for (Recognition recognition : list) {
                                    Debug.logD(TAG, "onOutput:" + recognition.title + ", confidence = " + recognition.confidence);
                                    if (recognition.confidence > confidence) {
                                        title = recognition.title;
                                        confidence = recognition.confidence;
                                    }
                                }
                                if (confidence > mConfidence) {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("title", title);
                                    bundle.putDouble("confidence", confidence);
                                    bundle.putString("frameId", outputData.frameId);
                                }
                            }
                            break;
                        }
                    }
                    //resultMap.containsKey(Constants.FACE_RECOGNITION)



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
