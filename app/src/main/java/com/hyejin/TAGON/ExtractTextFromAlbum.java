package com.hyejin.TAGON;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.IOException;


public class ExtractTextFromAlbum extends Activity {

    ImageView imgView;

    Uri uri;
    Bitmap bitmap;

    //opencv
    private static final String TAG = "opencv";
    private Mat matInput, matResult, matOriginal, textRoi;



    //opencv c++ 함수
    public native void Imageprocessing(long matAddrInput, long matAddrResult, long matAddrOriginal, long addrTextRoi);
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    Mat cropped;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extract_text_album);

        //xml 이미지 처리
        imgView = (ImageView) findViewById(R.id.img);

        // 갤러리 이미지 uri 받아오기 손실 발생햇는지 확인 필요
        uri=getIntent().getParcelableExtra("album_img");
        try {
            // 이미지 uri -> bitmap 변환
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //opencv test
        matInput= new Mat();
        Bitmap bitmapInput = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapInput, matInput);

        matResult= new Mat();
        matOriginal=new Mat();
        textRoi=new Mat();


        //((ExtractTextFromCamera)ExtractTextFromCamera.mContext).Imageprocessing(matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), matOriginal.getNativeObjAddr(), textRoi.getNativeObjAddr());
        Imageprocessing(matInput.getNativeObjAddr(), matResult.getNativeObjAddr(), matOriginal.getNativeObjAddr(), textRoi.getNativeObjAddr());

        //Mat bitmap 변환
        Bitmap bitmapOutput = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, bitmapOutput);
        imgView.setImageBitmap(bitmapOutput);

        //c++ 전처리 한번

        View view =findViewById(R.id.img);
        view.setOnTouchListener(new View.OnTouchListener() {


            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //c++ matInput
                int width = matInput.cols();
                int height = matInput.rows();

                ///안드로이드 터치 좌표 c++ 좌표로 전환
                int x =  (int) (width*event.getX()/imgView.getWidth());
                int y =  (int) (height*event.getY()/imgView.getHeight());

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

                            imgView.setImageBitmap(croppedOutput);

                            //MainActivity Tesseract에 넘기기
                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                            intent.putExtra("cropped_img",croppedOutput);
                            startActivity(intent);


                        }
                    }
                }

                return false;
            }
        });



    }

}
