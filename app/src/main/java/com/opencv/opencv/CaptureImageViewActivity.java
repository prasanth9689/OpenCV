package com.opencv.opencv;

import static org.opencv.imgproc.Imgproc.contourArea;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jsibbold.zoomage.ZoomageView;
import com.opencv.opencv.databinding.ActivityCaptureImageViewBinding;
import com.opencv.opencv.utils.ImageProcess;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaptureImageViewActivity extends AppCompatActivity {
    private ActivityCaptureImageViewBinding binding;
    private ZoomageView imageView ;
    private Bitmap mReceivedDocBitmap;
    private Bitmap mResultDocumentBitmap, mBitmapGray;
    private static final String LOGTAG = "OpenCV_Log";
    private final Context context = this;
    private final Mat mOriginalMat = new Mat();
    private List<MatOfPoint> contour = new ArrayList<>();
    Mat doc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCaptureImageViewBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        mReceivedDocBitmap = ImageProcess.byteArrayToBitmap(getIntent().getByteArrayExtra("data")); // received byte array to bitmap
        Utils.bitmapToMat(mReceivedDocBitmap, mOriginalMat); // received mat to bitmap convert

        /*
        test purpose to use local drawable

        Bitmap bMap=BitmapFactory.decodeResource(getResources(),R.drawable.test_2);
        Utils.bitmapToMat(bMap, originalMat);
         */

        Mat smallerImage = new Mat(new Size(mOriginalMat.width()/2, mOriginalMat.height()/2), mOriginalMat.type()); // new sized image

        Mat mGray = new Mat(); //Creating the empty destination matrix
        Mat mGassuianBlur = new Mat();
        Mat mImgThreshold = new Mat();
        Imgproc.cvtColor(mOriginalMat, mGray, Imgproc.COLOR_RGB2GRAY); // convert to gray

        org.opencv.core.Size s = new Size(5,5);  // apply gaussianBlur
        Imgproc.GaussianBlur(mGray, mGassuianBlur, s, 1);

        Imgproc.Canny(mGassuianBlur, mImgThreshold, 50, 200, 3, false); // apply canny

        Mat mHierarchey = new Mat(); // Image result for what is the hierarchy opencv android This way, contours in an image has some relationship to each other. And we can specify how one contour is connected to each other, like, is it child of some other contour, or is it a parent etc. Representation of this relationship is called the Hierarchy.
        Imgproc.findContours(mImgThreshold, contour, mHierarchey, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE); // find contour

        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contour.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contour.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        double ratio = 1;
        int height = Double.valueOf(mOriginalMat.size().height / ratio).intValue(); // get height and width
        int width = Double.valueOf(mOriginalMat.size().width / ratio).intValue();

        MatOfPoint temp_contour = contour.get(maxValIdx);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint largest_contour = contour.get(maxValIdx);
        //largest_contour.ge
        List<MatOfPoint> largest_contours = new ArrayList<MatOfPoint>();
        //Imgproc.drawContours(imgSource,contours, -1, new Scalar(0, 255, 0), 1);

                //check if this contour is a square
                MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                int contourSize = (int) temp_contour.total();
                MatOfPoint2f approxCurve_temp = new MatOfPoint2f();
                Imgproc.approxPolyDP(new_mat, approxCurve_temp, contourSize * 0.05, true);
                if (approxCurve_temp.total() == 4) {
                    approxCurve = approxCurve_temp;
                }else {
                    Toast.makeText(context, "Not found", Toast.LENGTH_SHORT).show();

                    Mat smallerImg=new Mat(new Size(mOriginalMat.width()/2, mOriginalMat.height()/2), mOriginalMat.type());

                    Mat gray=new Mat(mOriginalMat.size(), mOriginalMat.type());
                    Mat gray0=new Mat(mOriginalMat.size(),CvType.CV_8U);

                    // down-scale and upscale the image to filter out the noise
                    Imgproc.pyrDown(mOriginalMat, smallerImg, smallerImg.size());
                    Imgproc.pyrUp(smallerImg, mOriginalMat, mOriginalMat.size());
                    // find squares in every color plane of the image
                    for( int c = 0; c < 3; c++ )
                    {
                        extractChannel(mOriginalMat, gray, c);

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
                                      if (approx.total() == 4){
                                          // initialize src
                                          MatOfPoint2f dst = new MatOfPoint2f();
                                          approx.convertTo(dst, CvType.CV_32F);
                                          approxCurve = dst;
                                      }
                                }
                            }
                        }
                    }
                }

        double[] temp_double;
        temp_double = approxCurve.get(0,0);
        Point p1 = new Point(temp_double[0], temp_double[1]);
        //Core.circle(imgSource,p1,55,new Scalar(0,0,255));
        //Imgproc.warpAffine(sourceImage, dummy, rotImage,sourceImage.size());
        temp_double = approxCurve.get(1,0);
        Point p2 = new Point(temp_double[0], temp_double[1]);
        // Core.circle(imgSource,p2,150,new Scalar(255,255,255));
        temp_double = approxCurve.get(2,0);
        Point p3 = new Point(temp_double[0], temp_double[1]);
        //Core.circle(imgSource,p3,200,new Scalar(255,0,0));
        temp_double = approxCurve.get(3,0);
        Point p4 = new Point(temp_double[0], temp_double[1]);

        Point tl = p1;
        Point tr = p2;
        Point br = p3;
        Point bl = p4;

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB)*ratio;
        int maxWidth = Double.valueOf(dw).intValue();

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB)*ratio;
        int maxHeight = Double.valueOf(dh).intValue();

        doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0,
                tl.x*ratio, tl.y*ratio,
                tr.x*ratio, tr.y*ratio,
                br.x*ratio, br.y*ratio,
                bl.x*ratio, bl.y*ratio);

        dst_mat.put(0, 0,
                0.0, 0.0,
                dw, 0.0,
                dw, dh,
                0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Imgproc.warpPerspective(mOriginalMat, doc, m, doc.size());

        covertToGray();
        binding.imageView.setImageBitmap(mBitmapGray);

        new Thread(new Runnable() {
            public void run(){
               convertToTiff();
            }
        }).start();

        binding.closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void convertToTiff() {
        mResultDocumentBitmap = applyThreshold_fastNlMeansDenoising(doc); // apply threshold scanning look
    }

    private void covertToGray() {
        mBitmapGray = convertGrayImage(doc);
        saveImage(mBitmapGray);
    }

    private Bitmap showImage(Mat src){
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, bitmap);
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] byteArray1 = bos.toByteArray();
        return bitmap;
    }

    /**
     * Apply a threshold to give the "scanned" look
     *
     * NOTE:
     * See the following link for more info http://docs.opencv.org/3.1.0/d7/d4d/tutorial_py_thresholding.html#gsc.tab=0
     * @param src A valid Mat
     * @return The processed Bitmap
     */

    //below line added by prasanth
    /*
     * Apply a fastNlMeansDenoising to get a removed noise image
     *
     * https://docs.opencv.org/4.x/d1/d79/group__photo__denoise.html#ga76abf348c234cecd0faf3c42ef3dc715
     * https://stackoverflow.com/q/37915358/20621557
     */

    private Bitmap applyThreshold_fastNlMeansDenoising(Mat src) {
        Imgproc.GaussianBlur(src, src, new Size(5, 5), 0);
        Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2);

        Photo.fastNlMeansDenoising(src, src, 30.0f, 7, 21);
        Photo.fastNlMeansDenoising(src, src, 70.0f, 7, 21);

        Bitmap bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bm);

        return bm;
    }

    private Bitmap convertGrayImage(Mat src) {
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Bitmap bm = Bitmap.createBitmap(src.width(), src.height(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(src, bm);

        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f); // for horizontal
        matrix.postTranslate( 0, bm.getHeight());
        Bitmap scaledBitmap = Bitmap.createBitmap( bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        return scaledBitmap;
    }

    private void saveImage(Bitmap bitmap) {
        File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Cameo");
        if (!root.exists()) {
            root.mkdirs();
        } else {
            System.out.print("Exists");
        }

        File f = new File(root, getRandomString() + ".jpeg");
        //Convert bitmap to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        byte[] bitmapdata = bos.toByteArray();

        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //write the bytes in file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

    public void onClickReTake(View view) {
        finish();
    }

    public void onClickKeep(View view) {

        // for gray image to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        mBitmapGray.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] mGrayImageByteArray = bos.toByteArray();

        // for applied thrshold image to byte array
//        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
//        mResultDocumentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos1);
//        byte[] mApplyThrsholdImageByteArray = bos1.toByteArray();

        Intent intent = new Intent(context, SendDataActivity.class);
        intent.putExtra("data_gray_image", mGrayImageByteArray);
        intent.putExtra("data_thrs_image", mGrayImageByteArray);
        startActivity(intent);
    }
}