package com.opencv.opencv.utils;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ImageProcess{
    public static Bitmap byteArrayToBitmap(byte[] byteArray){
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }
}