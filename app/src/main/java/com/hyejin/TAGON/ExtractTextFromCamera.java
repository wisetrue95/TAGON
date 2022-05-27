package com.hyejin.TAGON;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Collections;
import java.util.List;

public class ExtractTextFromCamera extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    public static Context mContext;

    //opencv
    private static final String TAG = "opencv";
    private Mat matInput, matResult, matOriginal, textRoi,cropped;

    private CameraBridgeViewBase mOpenCvCameraView;


    //opencv c++ 함수
    public native void Imageprocessing(long matAddrInput, long matAddrResult, long matAddrOriginal, long addrTextRoi);

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setContentView(R.layout.extract_text_camera);
        mContext=this;

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mOpenCvCameraView.setOnTouchListener(this);




    }


    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //c++ matInput
        int width = matInput.cols()*5;
        int height = matInput.rows()*5;

        ///안드로이드 터치 좌표 c++ 좌표로 전환
        int x =  (int) (width*event.getX()/mOpenCvCameraView.getWidth());
        int y =  (int) (height*event.getY()/mOpenCvCameraView.getHeight());

        System.out.println("c++ x, c++ y");
        System.out.println(x);
        System.out.println(y);


        //textROI 좌표 체크
        for (int i = 0; i < textRoi.cols(); i++){
                    /*
                    textRoi.at<Vec4b>(0,i)[0] = left;
                    textRoi.at<Vec4b>(0, i)[1] = top;
                    textRoi.at<Vec4b>(0, i)[2] = width;
                    textRoi.at<Vec4b>(0, i)[3] = height;

                     */

            int cleft=(int)textRoi.get(0,i)[0];
            int ctop=(int)textRoi.get(0,i)[1];
            int cwidth=(int)textRoi.get(0,i)[2];
            int cheight=(int)textRoi.get(0,i)[3];


            //**조건확인 필요 0일때!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if(x>=cleft && x<=(cleft+cwidth)){
                if(y>=ctop && y<=(ctop+cheight)){

                    System.out.println("cleft");
                    System.out.println(cleft);

                    Rect rect = new Rect(cleft,ctop,cwidth,cheight);
                    //cropped=new Mat(matOriginal,rect);
                    cropped=new Mat(matOriginal,rect);


                    System.out.println("cropped.rows, cropped.cols");
                    System.out.println(cropped.rows());
                    System.out.println(cropped.cols());

                    //크롭 보여주기 cropped
                    Bitmap croppedOutput = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(cropped, croppedOutput);

                    //회전쓰 (카메라 할때만 회전)
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);

                    Bitmap rotatedBitmap = Bitmap.createBitmap(croppedOutput, 0, 0, croppedOutput.getWidth(), croppedOutput.getHeight(), matrix, true);

                   //imgView.setImageBitmap(croppedOutput);

                    //MainActivity Tesseract에 넘기기
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("cropped_img",rotatedBitmap);
                    startActivity(intent);


                }
            }
        }

        return false;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();

        //갑자기 꺼지는거 해결
        if ( matResult == null ){
            matResult = new Mat();
            matOriginal = new Mat();
            textRoi = new Mat();
        }

        Imageprocessing(matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), matOriginal.getNativeObjAddr(), textRoi.getNativeObjAddr());

        return matResult;

    }

    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }


    //-- 여기서부턴 퍼미션 관련 메소드
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS  = {"android.permission.CAMERA","android.permission.READ_EXTERNAL_STORAGE"
            , "android.permission.WRITE_EXTERNAL_STORAGE"};

    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){

            result = ContextCompat.checkSelfPermission(this, perms);

            if (result == PackageManager.PERMISSION_DENIED){
                //허가 안된 퍼미션 발견
                return false;
            }
        }

        //모든 퍼미션이 허가되었음
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){

            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted)
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder( ExtractTextFromCamera.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }



}
