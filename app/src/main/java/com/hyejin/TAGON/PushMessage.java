package com.hyejin.TAGON;
/*
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class PushMessage extends Activity {
    public static Context mContext;


    //푸시 버튼
    Button singerbutton;
    //텍스트뷰
    TextView singertextview1;
    //가격뷰
    TextView singertextview2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.singeritem);
        mContext=this;

        //id 맵핑
        //searchResult=findViewById(R.id.searchResult);
        singertextview1=findViewById(R.id.singertextview1);
        singertextview2=findViewById(R.id.singertextview2);
        singerbutton=findViewById(R.id.singerbutton);

    }


    public void pushmessage(){

        Toast.makeText(getApplicationContext(), "왜안돼", Toast.LENGTH_LONG).show();

        // 그리드 뷰 안에있는 품번 가져오기
        CharSequence previous_poombun= singertextview1.getText();

        //품번 가격 검색
        //naver 함수 되나? 확인
        String new_price=((SearchActivity)SearchActivity.mContext).NaverShopping(previous_poombun.toString());
        int new_price_int = Integer.parseInt(new_price);

        //이전 가격
        CharSequence previous_price= singertextview2.getText();
        int previous_price_int = Integer.parseInt(previous_price.toString());

        Toast.makeText(getApplicationContext(), new_price, Toast.LENGTH_LONG).show();



        //가격 전보다 작으면
        if(previous_price_int >= new_price_int){
            //갱신
            singertextview2.setText(new_price_int);

        }

    }
}


*/