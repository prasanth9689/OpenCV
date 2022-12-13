package com.opencv.opencv;

import static com.opencv.opencv.utils.OpenCvColorConstants.blue;

import static org.opencv.imgproc.Imgproc.contourArea;
import static org.opencv.imgproc.Imgproc.drawMarker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.opencv.opencv.databinding.MainActivityBinding;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends CameraActivity {
    private MainActivityBinding binding;
    private static  String LOGTAG = "OpenCV_Log";
    private static final int MY_WRITE_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_BACK;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Bitmap bitmap;
    private Mat mRGBA , mOutputRGBA;
    private TextView rectHeightText , rectWidthText , lookingDocumentText;
    private List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    private Rect rect;
    private Context context = this;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    Log.v(LOGTAG, "OpenCV Loaded");
                    binding.mOpenCvCameraView.enableView();
                }break;
                default:{
                    super.onManagerConnected(status);
                }break;
            }
            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FULL_SCREEN_REQUEST();
        binding = MainActivityBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        initActivity();
    }
    private void initActivity() {
        checkCameraPermission();
        initializeCamera();
        setOnClickListener();
    }

    @Override
    protected List<?extends CameraBridgeViewBase> getCameraViewList(){
        return Collections.singletonList(binding.mOpenCvCameraView);
    }

    private CameraBridgeViewBase.CvCameraViewListener2 cvCameraViewListener = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            mRGBA = new Mat(height, width, CvType.CV_8UC4);
            mOutputRGBA = new Mat(height, width, CvType.CV_8UC4);
        }

        @Override
        public void onCameraViewStopped() {
            mRGBA.release();
            mOutputRGBA.release();
        }

        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            mRGBA = inputFrame.rgba();

            if (Math.random()>0.80) {
                findSquares(inputFrame.rgba().clone(), contours);
            }

            mOutputRGBA = inputFrame.rgba().clone();

            Imgproc.drawContours(mRGBA, contours, 0, blue, 3);

            return mRGBA;
        }

        int thresh = 50, N = 11;

        // helper function:
        // finds a cosine of angle between vectors
        // from pt0->pt1 and from pt0->pt2
        double angle( Point pt1, Point pt2, Point pt0 ) {
            double dx1 = pt1.x - pt0.x;
            double dy1 = pt1.y - pt0.y;
            double dx2 = pt2.x - pt0.x;
            double dy2 = pt2.y - pt0.y;
            return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
        }

        // returns sequence of squares detected on the image.
        // the sequence is stored in the specified memory storage
        void findSquares(@NonNull Mat image, @NonNull List<MatOfPoint> squares )
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    squares.clear();

                    Mat smallerImg=new Mat(new Size(image.width()/2, image.height()/2),image.type());

                    Mat gray=new Mat(image.size(),image.type());
                    Mat gray0=new Mat(image.size(),CvType.CV_8U);

                    // down-scale and upscale the image to filter out the noise
                    Imgproc.pyrDown(image, smallerImg, smallerImg.size());
                    Imgproc.pyrUp(smallerImg, image, image.size());

                    // find squares in every color plane of the image
                    for( int c = 0; c < 3; c++ )
                    {
                        extractChannel(image, gray, c);

                        // try several threshold levels
                        for( int l = 1; l < N; l++ )
                        {
                            //Cany removed... Didn't work so well
                            Imgproc.threshold(gray, gray0, (l+1)*255/N, 255, Imgproc.THRESH_BINARY);
                            List<MatOfPoint> contours=new ArrayList<MatOfPoint>();
                            // find contours and store them all as a list
                            Imgproc.findContours(gray0, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                            MatOfPoint approx=new MatOfPoint();

                            // test each contour
                            for( int i = 0; i < contours.size(); i++ )
                            {
                                // approximate contour with accuracy proportional
                                // to the contour perimeter
                                approx = approxPolyDP(contours.get(i),  Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true)*0.02, true);
                                // square contours should have 4 vertices after approximation
                                // relatively large area (to filter out noisy contours)
                                // and be convex.
                                // Note: absolute value of an area is used because
                                // area may be positive or negative - in accordance with the
                                // contour orientation

                                if( approx.toArray().length == 4 &&
                                        Math.abs(contourArea(approx)) > 1000 &&
                                        Imgproc.isContourConvex(approx) )
                                {
                                    double maxCosine = 0;

                                    for( int j = 2; j < 5; j++ )
                                    {
                                        // find the maximum cosine of the angle between joint edges
                                        double cosine = Math.abs(angle(approx.toArray()[j%4], approx.toArray()[j-2], approx.toArray()[j-1]));
                                        maxCosine = Math.max(maxCosine, cosine);
                                    }
                                    // if cosines of all angles are small
                                    // (all angles are ~90 degree) then write quandrange
                                    // vertices to resultant sequence
                                    if( maxCosine < 0.3 )
                                        squares.add(approx);
                                }
                            }
                        }
                    }
                }
            }).start();
        }

        void extractChannel(Mat source, Mat out, int channelNum) {
            List<Mat> sourceChannels=new ArrayList<Mat>();
            List<Mat> outChannel=new ArrayList<Mat>();

            Core.split(source, sourceChannels);
            outChannel.add(new Mat(sourceChannels.get(0).size(),sourceChannels.get(0).type()));
            Core.mixChannels(sourceChannels, outChannel, new MatOfInt(channelNum,0));
            Core.merge(outChannel, out);
        }
        MatOfPoint approxPolyDP(MatOfPoint curve, double epsilon, boolean closed) {
            MatOfPoint2f tempMat=new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(curve.toArray()), tempMat, epsilon, closed);
            return new MatOfPoint(tempMat.toArray());
        }
    };

    private void setOnClickListener(){
        binding.captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (contours.isEmpty()) {
                    Log.d(LOGTAG, "Looking document");
                } else {
                    Log.d(LOGTAG, "document found");

                    rect = Imgproc.boundingRect(contours.get(0));
                    Mat ROI = mOutputRGBA.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);

                    try {
                        bitmap = Bitmap.createBitmap(mOutputRGBA.cols(), mOutputRGBA.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(mOutputRGBA, bitmap);
                    }catch(Exception ex){
                        System.out.println(ex.getMessage());
                    }

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    byte[] byteArray = bos.toByteArray();

                    Intent intent = new Intent(context, CaptureImageViewActivity.class);
                    intent.putExtra("data", byteArray);
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    }
                }

            int thresh = 50, N = 11;

            // helper function:
            // finds a cosine of angle between vectors
            // from pt0->pt1 and from pt0->pt2
            double angle( Point pt1, Point pt2, Point pt0 ) {
                double dx1 = pt1.x - pt0.x;
                double dy1 = pt1.y - pt0.y;
                double dx2 = pt2.x - pt0.x;
                double dy2 = pt2.y - pt0.y;
                return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
            }

            // returns sequence of squares detected on the image.
            // the sequence is stored in the specified memory storage
            void findSquares(@NonNull Mat image, @NonNull List<MatOfPoint> squares )
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        squares.clear();

                        Mat smallerImg=new Mat(new Size(image.width()/2, image.height()/2),image.type());

                        Mat gray=new Mat(image.size(),image.type());
                        Mat gray0=new Mat(image.size(),CvType.CV_8U);

                        // down-scale and upscale the image to filter out the noise
                        Imgproc.pyrDown(image, smallerImg, smallerImg.size());
                        Imgproc.pyrUp(smallerImg, image, image.size());

                        // find squares in every color plane of the image
                        for( int c = 0; c < 3; c++ )
                        {
                            extractChannel(image, gray, c);

                            // try several threshold levels
                            for( int l = 1; l < N; l++ )
                            {
                                //Cany removed... Didn't work so well
                                Imgproc.threshold(gray, gray0, (l+1)*255/N, 255, Imgproc.THRESH_BINARY);
                                List<MatOfPoint> contours=new ArrayList<MatOfPoint>();
                                // find contours and store them all as a list
                                Imgproc.findContours(gray0, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                                MatOfPoint approx=new MatOfPoint();

                                // test each contour
                                for( int i = 0; i < contours.size(); i++ )
                                {
                                    // approximate contour with accuracy proportional
                                    // to the contour perimeter
                                    approx = approxPolyDP(contours.get(i),  Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true)*0.02, true);
                                    // square contours should have 4 vertices after approximation
                                    // relatively large area (to filter out noisy contours)
                                    // and be convex.
                                    // Note: absolute value of an area is used because
                                    // area may be positive or negative - in accordance with the
                                    // contour orientation

                                    if( approx.toArray().length == 4 &&
                                            Math.abs(contourArea(approx)) > 1000 &&
                                            Imgproc.isContourConvex(approx) )
                                    {
                                        double maxCosine = 0;

                                        for( int j = 2; j < 5; j++ )
                                        {
                                            // find the maximum cosine of the angle between joint edges
                                            double cosine = Math.abs(angle(approx.toArray()[j%4], approx.toArray()[j-2], approx.toArray()[j-1]));
                                            maxCosine = Math.max(maxCosine, cosine);
                                        }
                                        // if cosines of all angles are small
                                        // (all angles are ~90 degree) then write quandrange
                                        // vertices to resultant sequence
                                        if( maxCosine < 0.3 )
                                            squares.add(approx);
                                    }
                                }
                            }
                        }
                    }
                }).start();
            }

            void extractChannel(Mat source, Mat out, int channelNum) {
                List<Mat> sourceChannels=new ArrayList<Mat>();
                List<Mat> outChannel=new ArrayList<Mat>();

                Core.split(source, sourceChannels);
                outChannel.add(new Mat(sourceChannels.get(0).size(),sourceChannels.get(0).type()));
                Core.mixChannels(sourceChannels, outChannel, new MatOfInt(channelNum,0));
                Core.merge(outChannel, out);
            }
            MatOfPoint approxPolyDP(MatOfPoint curve, double epsilon, boolean closed) {
                MatOfPoint2f tempMat=new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(curve.toArray()), tempMat, epsilon, closed);
                return new MatOfPoint(tempMat.toArray());
            }
        });
    }

    private void findContourAgain() {
        rect = Imgproc.boundingRect(contours.get(0));

                    Mat ROI = mOutputRGBA.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
                    Bitmap bitmap = null;
                    try {
                        bitmap = Bitmap.createBitmap(ROI.cols(), ROI.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(ROI, bitmap);
                    }catch(Exception ex){
                        System.out.println(ex.getMessage());
                    }
                    binding.imageView.setImageBitmap(bitmap);
    }

    private void showLookingDocument() {
        lookingDocumentText.setVisibility(View.VISIBLE);
    }

    private void initializeCamera() {
        binding.mOpenCvCameraView.setCameraPermissionGranted();
        binding.mOpenCvCameraView.setCameraIndex(activeCamera);
        binding.mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        binding.mOpenCvCameraView.setMaxFrameSize(1250,720);
        binding.mOpenCvCameraView.setCvCameraViewListener(cvCameraViewListener);
        binding.mOpenCvCameraView.setEnabled(true);
    }

    private void saveImage(Bitmap bitmap) {
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Cameo");
        if (!root.exists()) { root.mkdirs(); } else { System.out.print("Exists");}

        File f = new File(root, getRandomString() + ".jpeg");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] bitmapdata = bos.toByteArray();

        try { f.createNewFile();} catch (IOException e) {e.printStackTrace();}
        //write the bytes in file
        FileOutputStream fos = null;
        try { fos = new FileOutputStream(f); fos.write(bitmapdata); fos.flush(); fos.close();
        } catch (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace();}}

    @Override
    public void onPause(){ super.onPause(); if (binding.mOpenCvCameraView != null){ binding.mOpenCvCameraView.disableView(); }}

    @Override
    public void onDestroy(){ super.onDestroy(); if (binding.mOpenCvCameraView != null){ binding.mOpenCvCameraView.disableView(); }}

    @Override
    public void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            Log.d(LOGTAG, "OpenCV not found, initializing");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();

        }
    }

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            Log.d(LOGTAG, "Storage permission granted");
        }else {
            Log.d(LOGTAG, "Storage permission required");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOGTAG, "Permission granted");
            initializeCamera();
        } else {
            Log.d(LOGTAG, "Permisson required");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
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

//    private void findLargestContourWorkingRe(){
//                    for (int contourIdx = 0; contourIdx < squares.size(); contourIdx++){
//
//                        // Minimum size allowed consideration
//                        MatOfPoint2f approxCurve = new MatOfPoint2f();
//                        MatOfPoint2f contour2f = new MatOfPoint2f(squares.get(contourIdx).toArray());
//
//                        // Processing on mMao2fl which is type MatPoint2f
//                        double approxDistance = Imgproc.arcLength(contour2f, true) *0.01;
//                        Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
//
//                        // Convert back to MatOfPoint
//                        MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//
//                        // Get bounding rect of contour
//                        rect = Imgproc.boundingRect(points);
//
//                        double height = rect.height;
//                        double width = rect.width;
//
//                        runOnUiThread(new Runnable() {
//                            public void run() {
//                                rectHeightText.setText(String.valueOf(height));
//                                rectWidthText.setText(String.valueOf(width));
//                            }
//                        });
//                    }
//    }

    private void FULL_SCREEN_REQUEST() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}