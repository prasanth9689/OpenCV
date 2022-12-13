package com.opencv.opencv;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.opencv.opencv.databinding.ActivitySendDataBinding;
import com.opencv.opencv.databinding.MainActivityBinding;
import com.opencv.opencv.utils.ImageProcess;

import org.beyka.tiffbitmapfactory.CompressionScheme;
import org.beyka.tiffbitmapfactory.IProgressListener;
import org.beyka.tiffbitmapfactory.Orientation;
import org.beyka.tiffbitmapfactory.TiffBitmapFactory;
import org.beyka.tiffbitmapfactory.TiffConverter;
import org.beyka.tiffbitmapfactory.TiffSaver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

public class SendDataActivity extends AppCompatActivity {
    private ImageView grayImageView , mTiffImageView;
    private Bitmap mBitmapGray, mBitmapTiff;
    private ActivitySendDataBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySendDataBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mBitmapGray = ImageProcess.byteArrayToBitmap(getIntent().getByteArrayExtra("data_gray_image"));
        mBitmapTiff = ImageProcess.byteArrayToBitmap(getIntent().getByteArrayExtra("data_thrs_image"));

        binding.grayImageView.setImageBitmap(mBitmapGray);

        saveTiffDocument();

        binding.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void saveTiffDocument() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Cameo/" + getRandomString() +".tif");
                TiffSaver.SaveOptions options = new TiffSaver.SaveOptions();
                options.compressionScheme = CompressionScheme.CCITTRLE; //By default compression mode is none
                options.orientation = Orientation.TOP_LEFT; // By default orientation is top left
                options.author = "Cameo";
                options.copyright = "Cameo corporate service limited";
                TiffSaver.saveBitmap(root, mBitmapTiff, options);

                binding.tiffImageView.setImageBitmap(mBitmapTiff);
            }
        });
        thread.start();
         //To stop thread just interrupt thread as usual
        thread.interrupt();
    }
    private static String getRandomString()
    {
        final String ALLOWED_CHARACTERS ="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        final Random random=new Random();
        final StringBuilder sb=new StringBuilder(16);
        for(int i = 0; i< 16; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        return sb.toString();
    }
}