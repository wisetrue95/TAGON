package com.hyejin.TAGON;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class SingerViewer extends LinearLayout {

    TextView singertextview1;
    TextView singertextview2;
    ImageView singerimageview1;

    public SingerViewer(Context context) {
        super(context);

        init(context);
    }

    public SingerViewer(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);

        init(context);
    }

    public void init(Context context){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.singeritem,this,true);

        singertextview1 = findViewById(R.id.singertextview1);
        singertextview2 = findViewById(R.id.singertextview2);
        singerimageview1 = findViewById(R.id.singerimageview1);
    }

    public void setItem(SingerItem singerItem){
        singertextview1.setText(singerItem.getName());
        singertextview2.setText(singerItem.getprice());
        singerimageview1.setImageBitmap(singerItem.getImage());
    }


}


