package com.hyejin.TAGON;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;


public class Poombun extends Activity {

    String poombun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.poompun_popup);

        // UI 객체생성
        TextView checkPoompun = findViewById(R.id.checkPoompun);

        // 품번 데이터 가져오기
        Intent intent = getIntent();
        poombun = intent.getExtras().getString("poombun");
        poombun= poombun.replace("-", " ");

        checkPoompun.setText(poombun);

    }



    //--확인 버튼
    public void mOnClose(View v){
        // SearchActivity로 뷰 넘기기
        Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
        intent.putExtra("poombun", poombun);
        startActivity(intent);

        // 액티비티(팝업) 닫기
        finish();

    }

    //--취소 버튼
    public void Cancellation(View v){
        //액 티비티(팝업) 닫기
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 바깥레이어 클릭시 안닫히게
        if(event.getAction()==MotionEvent.ACTION_OUTSIDE){
            return false;
        }
        return true;
    }

}
