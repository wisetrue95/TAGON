package com.hyejin.TAGON;

import android.graphics.Bitmap;

public class SingerItem {

    private String name;
    private String price;
    private Bitmap image;

    public SingerItem(String name, String price, Bitmap image) {
        this.name = name;
        this.price = price;
        this.image = image;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getprice() {
        return price;
    }

    public void setprice(String price) {
        this.price = price;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }
}
