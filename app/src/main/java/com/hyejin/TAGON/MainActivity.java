package com.hyejin.TAGON;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.theartofdev.edmodo.cropper.CropImage;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Tagon 3.1 크롭 기능 추가
public class MainActivity extends AppCompatActivity{



    // 테서렉트 학습데이터
    TessBaseAPI tessBaseAPI;
    String lang = "eng+tag50";    //tagon: 400개 학습
    String lang_one[] = lang.split("\\+");

    String poombun;

    // xml
    Button btnCamera, gallery,  wishList;
    ImageView img, img2;
    TextView textView2;
    SearchView searchView;

    Uri photoUri;
    Bitmap bitmap;

    // request code
    private static final int PICK_FROM_CAMERA=1;
    private static final int PICK_FROM_AlBUM=2;

    // openCV 테서렉트 전처리
    private Mat matInput, matResult;


    public native void Imageprocessing(long matAddrInput, long matAddrResult);
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    // main
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {

                //퍼미션 허가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        // xml
        btnCamera = (Button) findViewById(R.id.btnCamera);
        gallery = (Button) findViewById(R.id.gallery);
        img = (ImageView) findViewById(R.id.img);
        img2 = (ImageView) findViewById(R.id.img2);
        textView2 = findViewById(R.id.textView2);
        wishList = (Button) findViewById(R.id.wishList);
        searchView = findViewById(R.id.searchview);
        searchView.setIconified(false);
        searchView.clearFocus();



        //--버튼1) 카메라
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IsCameraAvailable()) {
                    //getImageCamera();  // 카메라 촬영 이미지

                    Intent intent = new Intent(getApplicationContext(), ExtractTextFromCamera.class);
                    //intent.putExtra("album_img",photoUri);
                    startActivity(intent);

                }
            }
        });

        //--버튼2) 갤러리
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IsCameraAvailable()) {
                    getImageAlbum(); // 이미지 불러오기 함수



                }
            }
        });


        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                poombun=query;
                //검색 버튼이 눌러졌을 때 이벤트 처리
                Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                intent.putExtra("poombun", poombun);
                startActivity(intent);
                //finish();
                return true;
            }
            public boolean onQueryTextChange(String newText){
                return false;
            }

        });




        //--버튼4) 위시리스트
        wishList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //뷰 넘기기
                //Intent intent = new Intent(getApplicationContext(), Favorites.class);
                Intent intent = new Intent(getApplicationContext(), Favorites.class);
                startActivity(intent);
            }
        });


        // 테서렉트
        tessBaseAPI = new TessBaseAPI();
        String dir = getFilesDir() + "/tesseract";
        if(checkLanguageFile(dir+"/tessdata")) {
            tessBaseAPI.init(dir, lang);
            tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"1234567890"+"ABCDEFGHIJKLMNOPRSTUVWXYZ"+"-/"
                    +"abcdefghijklmnopqrstuvwxyz");

        }

        Bitmap bitmapOutput=getIntent().getParcelableExtra("cropped_img");
        if(bitmapOutput!=null){
            img2.setImageBitmap(bitmapOutput);

            //테서렉트 실행
            new AsyncTess().execute(bitmapOutput);
        }

    }


    //--함수

    //--테서렉트
    boolean checkLanguageFile(String dir)
    {
        for(String one : lang_one) {
            File file = new File(dir);
            if (!file.exists() && file.mkdirs())
                createFiles(dir);
            else if (file.exists()) {
                String filePath = dir + "/" + one + ".traineddata";
                File langDataFile = new File(filePath);
                if (!langDataFile.exists())
                    createFiles(dir);
            }
        }
        return true;
    }

    // 테서렉트
    private void createFiles(String dir)
    {
        for(String one : lang_one) {
            AssetManager assetMgr = this.getAssets();

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = assetMgr.open(one + ".traineddata");

                String destFile = dir + "/" + one + ".traineddata";

                outputStream = new FileOutputStream(destFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    // 카메라 촬영 이미지 가져오기
    private void getImageCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();  // 파일 생성 함수
            } catch (IOException ex) {
                // Error occurred while creating the File
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, PICK_FROM_CAMERA); // 카메라에서 찍은 이미지 가져오기

            }
        }
    }

    // 갤러리에서 이미지 불러오기
    private void getImageAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent,PICK_FROM_AlBUM);
    }


    // request 코드 받아서 수행하는 단계
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            // 카메라 촬영 이미지 가져오기: getImageCamera()
            case PICK_FROM_CAMERA: {

                //CropImage.activity(photoUri).start(this);
                Intent intent = new Intent(getApplicationContext(), ExtractTextFromAlbum.class);
                intent.putExtra("album_img",photoUri);
                startActivity(intent);
                break;
            }

            // 갤러리에서 이미지 불러오기: getImageAlbum()
            case PICK_FROM_AlBUM: {
                photoUri = data.getData();

                CropImage.activity(photoUri).start(this);
                break;
            }

            //** 크롭이 아니라 터치 입력 받기 (카메라접근은 따로 구현 해야함)
            // Cropper API 크롭
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE: {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    photoUri = result.getUri();
                    img.setImageURI(photoUri);

                    try {
                        // 이미지 uri -> bitmap 변환
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //img2.setImageBitmap(bitmap);

                    // opencv 전처리
                    opencvProcess();

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }


        }
    }


    // 뒤로가기 종료
    @Override public void onBackPressed() {
        moveTaskToBack(true);                 // 태스크를 백그라운드로 이동
        finishAndRemoveTask();                         // 액티비티 종료 + 태스크 리스트에서 지우기
        android.os.Process.killProcess(android.os.Process.myPid()); // 앱 프로세스 종료

    }

    // 카메라 유무
    public boolean IsCameraAvailable(){
        PackageManager packageManager = getPackageManager();

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    // 이미지 파일 생성
    private String imageFilePath;
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "TEST_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",         /* suffix */
                storageDir          /* directory */
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }


    // 테서렉트 이미지 추출
    private class AsyncTess extends AsyncTask<Bitmap, Integer, String> {
        @Override
        protected String doInBackground(Bitmap... mRelativeParams) {
            tessBaseAPI.setImage(mRelativeParams[0]);
            return tessBaseAPI.getUTF8Text();
        }

        protected void onPostExecute(String result) {
            //Toast.makeText(MainActivity.this, "인식중입니다.", Toast.LENGTH_LONG).show();
            textView2.setText(result);
            poombun=result;

            //팝업창 뷰 넘기기
            //Intent intent = new Intent(getApplicationContext(), Poombun.class);
            //intent.putExtra("poombun", poombun);
            //startActivityForResult(intent, 1);
            searchView.setQuery(result,false);


        }
    }

    // opencv 전처리
    private void opencvProcess(){


        matInput= new Mat();
        Bitmap bitmapInput = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bitmapInput, matInput);

        matResult= new Mat();


        // 흑백 변환
        Imageprocessing(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());



        Bitmap bitmapOutput = Bitmap.createBitmap(matResult.cols(), matResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matResult, bitmapOutput);


        img2.setImageBitmap(bitmapOutput);

        //테서렉트 실행
        new AsyncTess().execute(bitmapOutput);

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

        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
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

