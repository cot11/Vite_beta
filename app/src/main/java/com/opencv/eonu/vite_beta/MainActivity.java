package com.opencv.eonu.vite_beta;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java");
    }
    private static final int PICK_FROM_CAMERA = 0;
    private static final int PICK_FROM_ALBUM = 1;
    private static final int CROP_FROM_IMAGE = 2;

    private Uri CaptureUri;
    ImageView user_face, user_face2;
    Button btn, btn2, btn3, btn4;
    Bitmap upload_bm = null;
    Bitmap upload_bm2 = null;
    boolean select = false;
    Context context;
    Mat rgb;


    boolean first_cut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        setContentView(R.layout.activity_main);
        btn = (Button)findViewById(R.id.click);
        btn2 = (Button)findViewById(R.id.click2);
        btn3 = (Button)findViewById(R.id.click3);
        btn4 = (Button)findViewById(R.id.click4);

        user_face = (ImageView) findViewById(R.id.user_face); // 이미지 뷰
        user_face2 = (ImageView) findViewById(R.id.user_face2);
        context = this;

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 배경지우기
                if(upload_bm != null)
                {
                    View cview = getCurrentFocus();
                    face_dectect(cview);
                }
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // intent
                if(upload_bm == null)
                {
                    return;
                }
                Intent intent=new Intent(MainActivity.this,objectcut.class);
                intent.putExtra("image", upload_bm);
                startActivity(intent);
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 피부색 검출
                if(upload_bm == null || upload_bm2 == null)
                {
                    return;
                }
                View cview = getCurrentFocus();
                skin_dectect(cview);


            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 옷 분리
                if(upload_bm == null)
                {
                    return;
                }
                View cview = getCurrentFocus();
                Dection(cview);

            }
        });

        user_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select = true;
                DialogInterface.OnClickListener cameraListner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TakePicture();
                    }
                };
                DialogInterface.OnClickListener albumListner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Album();
                    }
                };
                DialogInterface.OnClickListener cancelListner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                };

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("업로드할 이미지 선택")
                        .setNegativeButton("사진촬영", cameraListner)
                        .setPositiveButton("앨범선택", albumListner)
                        .setNeutralButton("취소", cancelListner)
                        .show();

            }
        });

        user_face2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                select = false;
                DialogInterface.OnClickListener cameraListner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        TakePicture();
                    }
                };
                DialogInterface.OnClickListener albumListner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Album();
                    }
                };
                DialogInterface.OnClickListener cancelListner = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                };

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("업로드할 이미지 선택")
                        .setNegativeButton("사진촬영", cameraListner)
                        .setPositiveButton("앨범선택", albumListner)
                        .setNeutralButton("취소", cancelListner)
                        .show();

            }
        });

    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            System.out.println("RESULT OK");
            return;
        }

        switch (requestCode) {
            case PICK_FROM_ALBUM: {

                CaptureUri = data.getData();
                File original_file = getImageFile(CaptureUri);
                try {
                    if(select)
                    {
                        upload_bm = MediaStore.Images.Media.getBitmap(getContentResolver(),CaptureUri);
                        Bitmap dstBmp = Bitmap.createBitmap(upload_bm,0,0,upload_bm.getWidth(),upload_bm.getHeight());
                        user_face.setImageDrawable(new BitmapDrawable(getResources(), dstBmp));
                    }
                    else
                    {
                        upload_bm2 = MediaStore.Images.Media.getBitmap(getContentResolver(),CaptureUri);
                        Bitmap dstBmp2 = Bitmap.createBitmap(upload_bm2,0,0,upload_bm2.getWidth(),upload_bm2.getHeight());
                        user_face2.setImageDrawable(new BitmapDrawable(getResources(), dstBmp2));
                    }



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case PICK_FROM_CAMERA: {

            }
        }

    }


    private void Dection(View v)
    {

        float x1 = 0,x2 = 0,y1 = 0,y2 = 0;
        float mouse_x = 0.0f;
        float mouse_y = 0.0f;
        float nose_x = 0.0f;
        float nose_y = 0.0f;


        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap myBitmap = upload_bm;
        Bitmap myBitmap2 = upload_bm2;

        //Rect Point color
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        //circle color paint
        Paint Pnt = new Paint();
        Pnt.setColor(Color.GREEN);
        Pnt.setStrokeWidth(5);
        Pnt.setStyle(Paint.Style.STROKE);

        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //얼굴 인식 부분
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(v.getContext()).setMessage("얼굴 검출 실패(얼굴 없음)");
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = null;
        try
        {
            faces = faceDetector.detect(frame);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if(faces.size() == 0)
        {
            System.out.println("얼굴 없는 이미지");
            return;
        }
        else if(faces.size() > 1)
        {
            System.out.println("얼굴 많음");
            return;
        }

        Face thisFace = faces.valueAt(0);
        x1 = thisFace.getPosition().x;
        y1 = thisFace.getPosition().y;
        x2 = x1 + thisFace.getWidth();
        y2 = y1 + thisFace.getHeight();
        if(x1 < 0)
        {
            x1 = 0;
        }
        if(y1 < 0)
        {
            y1 = 0;
        }
        int face_getwid = (int)thisFace.getWidth();
        int face_gethei = (int)thisFace.getHeight();
        if(x2 > myBitmap.getWidth())
        {
            face_getwid = myBitmap.getWidth() - (int)x1;
        }
        if(y2 > myBitmap.getHeight())
        {
            face_gethei = myBitmap.getHeight() - (int)y1;
        }

        
        tempCanvas.drawRoundRect(new RectF(x1,y1,x2,y2), 2, 2, myRectPaint);

        //draw landmarks
        List<Landmark> landmarks = thisFace.getLandmarks();
        //for make landmark circle
        Landmark thisLand = landmarks.get(2);
        nose_x = thisLand.getPosition().x;
        nose_y = thisLand.getPosition().y;
        tempCanvas.drawCircle(nose_x, nose_y, 10, Pnt);

        thisLand = landmarks.get(7);
        mouse_x = thisLand.getPosition().x;
        mouse_y = thisLand.getPosition().y;
        tempCanvas.drawCircle(mouse_x, mouse_y, 10, Pnt);

        System.out.println("nose_x : " + nose_x);
        System.out.println("mouse_x : " + mouse_x);

        Bitmap dstBmp = Bitmap.createBitmap(tempBitmap,(int)x1,(int)y1,face_getwid,face_gethei);
        Bitmap dstBmp2 = Bitmap.createBitmap(dstBmp);

        user_face2.setImageDrawable(new BitmapDrawable(getResources(), dstBmp2));

        Mat face1_rgb = new Mat();
        Utils.bitmapToMat(dstBmp,face1_rgb);
        //face1_rgb = grabcut(face1_rgb);
        Utils.matToBitmap(face1_rgb,dstBmp);
        user_face.setImageDrawable(new BitmapDrawable(getResources(), dstBmp));


        // 얼굴 목 연결
        /*
        // 두이미지 색 비교 소스
        Mat face_rgb = new Mat();
        Mat body_rgb = new Mat();
        Utils.bitmapToMat(upload_bm,face_rgb);
        Utils.bitmapToMat(upload_bm2,body_rgb);

        double face_R = face_rgb.get(0,0)[0];
        double face_G = face_rgb.get(0,0)[1];
        double face_B = face_rgb.get(0,0)[2];

        System.out.println("or : R : " + face_R);
        System.out.println("or : G : " + face_G);
        System.out.println("or : B : " + face_B);


        Imgproc.cvtColor(face_rgb,face_rgb,Imgproc.COLOR_BGR2HSV);

        double face_H = face_rgb.get(0,0)[0];
        double face_S = face_rgb.get(0,0)[1];
        double face_V = face_rgb.get(0,0)[2];

        System.out.println("or : H : " + face_H);
        System.out.println("or : S : " + face_S);
        System.out.println("or : V : " + face_V);

        Imgproc.cvtColor(face_rgb,face_rgb,Imgproc.COLOR_HSV2BGR);

        face_R = face_rgb.get(0,0)[0];
        face_G = face_rgb.get(0,0)[1];
        face_B = face_rgb.get(0,0)[2];

        System.out.println("or : R : " + face_R);
        System.out.println("or : G : " + face_G);
        System.out.println("or : B : " + face_B);


        Imgproc.cvtColor(body_rgb,body_rgb,Imgproc.COLOR_BGR2HSV);

        double H = body_rgb.get(0,0)[0];
        double S = body_rgb.get(0,0)[1];
        double V = body_rgb.get(0,0)[2];

        double[] data = new double[3];
        data[0] = face_H - 2.0;
        data[1] = face_S + 25.0;
        data[2] = face_V - 63.0;

        for(int r = 0; r < body_rgb.rows(); r++)
        {
            for(int c = 0; c < body_rgb.cols(); c++)
            {
                body_rgb.put(r,c,data);
            }
        }

        Imgproc.cvtColor(body_rgb,body_rgb,Imgproc.COLOR_HSV2BGR);
        Utils.matToBitmap(body_rgb,upload_bm2);

        double R = body_rgb.get(0,0)[0];
        double G = body_rgb.get(0,0)[1];
        double B = body_rgb.get(0,0)[2];

        System.out.println("R : " + R);
        System.out.println("G : " + G);
        System.out.println("B : " + B);

        */

    }
    private void skin_dectect(View v)
    {
        float x1 = 0,x2 = 0,y1 = 0,y2 = 0;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap myBitmap = upload_bm;
        Bitmap myBitmap2 = upload_bm2;

        //Rect Point color
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        //circle color paint
        Paint Pnt = new Paint();
        Pnt.setColor(Color.GREEN);
        Pnt.setStrokeWidth(5);
        Pnt.setStyle(Paint.Style.STROKE);

        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //얼굴 인식 부분
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(v.getContext()).setMessage("얼굴 검출 실패(얼굴 없음)");
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = null;
        try
        {
            faces = faceDetector.detect(frame);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if(faces.size() == 0)
        {
            System.out.println("얼굴 없는 이미지");
            return;
        }
        else if(faces.size() > 1)
        {
            System.out.println("얼굴 많음");
            return;
        }

        rgb = new Mat();
        Utils.bitmapToMat(myBitmap,rgb);
        Imgproc.cvtColor(rgb,rgb,Imgproc.COLOR_BGRA2BGR);
        Mat g_mask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        int rect_width = (int)(rgb.width()*0.95);
        int rect_height = (int)(rgb.height()*0.95);
        Rect rect = new Rect(10, 10,rect_width,rect_height);
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
        Imgproc.grabCut(rgb, g_mask, rect, bgModel, fgModel, 2, Imgproc.GC_INIT_WITH_RECT);
        Core.compare(g_mask, source, g_mask, Core.CMP_EQ);
        Mat fg = new Mat(rgb.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
        rgb.copyTo(fg, g_mask);

        Utils.matToBitmap(fg,myBitmap);

        Face thisFace = faces.valueAt(0);
        x1 = thisFace.getPosition().x;
        y1 = thisFace.getPosition().y;
        x2 = x1 + thisFace.getWidth();
        y2 = y1 + thisFace.getHeight();
        if(x1 < 0)
        {
            x1 = 0;
        }
        if(y1 < 0)
        {
            y1 = 0;
        }
        int face_getwid = (int)thisFace.getWidth();
        int face_gethei = (int)thisFace.getHeight();
        if(x2 > myBitmap.getWidth())
        {
            face_getwid = myBitmap.getWidth() - (int)x1;
        }
        if(y2 > myBitmap.getHeight())
        {
            face_gethei = myBitmap.getHeight() - (int)y1;
        }
        Bitmap Face_bitmap = Bitmap.createBitmap(myBitmap,(int)x1,(int)y1,face_getwid,face_gethei);
        tempCanvas.drawRoundRect(new RectF(x1,y1,x2,y2), 2, 2, myRectPaint);

        //draw landmarks
        List<Landmark> landmarks = thisFace.getLandmarks();
        //for make landmark circle
        for(int j=0; j<landmarks.size(); j++){
            Landmark thisLand = landmarks.get(j);
            float landx = thisLand.getPosition().x;
            float landy = thisLand.getPosition().y;
            tempCanvas.drawCircle(landx, landy, 10, Pnt);
        }

        user_face.setImageDrawable(new BitmapDrawable(getResources(), Face_bitmap));
        user_face2.setImageDrawable(new BitmapDrawable(getResources(), myBitmap2));

        Mat face_rgb = new Mat();
        Mat body_rgb = new Mat();
        Mat face_hsv = new Mat();
        Mat body_hsv = new Mat();
        Utils.bitmapToMat(Face_bitmap,face_rgb);
        Utils.bitmapToMat(myBitmap2,body_rgb);

        Imgproc.cvtColor(face_rgb,face_rgb,Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(body_rgb,body_rgb,Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(face_rgb,face_hsv,Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(body_rgb,body_hsv,Imgproc.COLOR_BGR2HSV);


        byte[] data = new byte[3];
        data[0] = (byte)255;
        data[1] = (byte)255;
        data[2] = (byte)255;


        Map<Integer , Integer> face_map_H = new HashMap<Integer , Integer>();
        Map<Integer , Integer> body_map_H = new HashMap<Integer , Integer>();
        Map<Integer , Integer> face_map_S = new HashMap<Integer , Integer>();
        Map<Integer , Integer> body_map_S = new HashMap<Integer , Integer>();
        Map<Integer , Integer> face_map_V = new HashMap<Integer , Integer>();
        Map<Integer , Integer> body_map_V = new HashMap<Integer , Integer>();

        int face_count = 0;
        int body_count = 0;

        //Face
        for(int r = 0; r < face_rgb.rows(); r++)
        {
            for(int c = 0; c < face_rgb.cols(); c++)
            {
                int R = (int)face_rgb.get(r,c)[0];
                int G = (int)face_rgb.get(r,c)[1];
                int B = (int)face_rgb.get(r,c)[2];

                int H = (int)face_hsv.get(r,c)[0];
                int S = (int)face_hsv.get(r,c)[1];
                int V = (int)face_hsv.get(r,c)[2];

                double var_R = (R / 255.0);
                double var_G = 0;
                double var_B = 0;
                if(G != 0)
                {
                    var_G = (G / 255.0);
                }
                if(B != 0)
                {
                    var_B = (B / 255.0);
                }

                double var_Min = Math.min(var_R,var_G);
                var_Min = Math.min(var_Min,var_B);
                double var_Max = Math.max( var_R, var_G);
                var_Max = Math.max(var_Max,var_G);
                double del_Max = var_Max - var_Min;
                double val_H = 0;
                double val_V = var_Max;

                if ( del_Max == 0 )
                {
                    val_H = 0;
                }
                else
                {
                    double del_R = ( ( ( var_Max - var_R ) / 6 ) + ( del_Max / 2 ) ) / del_Max;
                    double del_G = ( ( ( var_Max - var_G ) / 6 ) + ( del_Max / 2 ) ) / del_Max;
                    double del_B = ( ( ( var_Max - var_B ) / 6 ) + ( del_Max / 2 ) ) / del_Max;
                    if      ( var_R == var_Max ) val_H = del_B - del_G;
                    else if ( var_G == var_Max ) val_H = ( 1 / 3 ) + del_R - del_B;
                    else if ( var_B == var_Max ) val_H = ( 2 / 3 ) + del_G - del_R;
                    if ( val_H < 0 ) val_H += 1;
                    else if ( val_H > 1 ) val_H -= 1;
                }
                if(val_H >= 0.035 && val_H <= 0.09 && val_V > 0.65)
                {
                    face_count++;

                    if(!(face_map_H.containsKey(H))){
                        face_map_H.put(H,1);
                    }
                    else
                    {
                        face_map_H.put(H, face_map_H.get(H) + 1);
                    }

                    if(!(face_map_S.containsKey(S))){
                        face_map_S.put(S,1);
                    }
                    else
                    {
                        face_map_S.put(S, face_map_S.get(S) + 1);
                    }

                    if(!(face_map_V.containsKey(V))){
                        face_map_V.put(V,1);
                    }
                    else
                    {
                        face_map_V.put(V, face_map_V.get(V) + 1);
                    }
                }
                else
                {
                    face_rgb.put(r,c,data);
                }
            }
        }
        //Face end

        Utils.matToBitmap(face_rgb,Face_bitmap);

        //Body
        for(int r = 0; r < body_rgb.rows(); r++)
        {
            for(int c = 0; c < body_rgb.cols(); c++)
            {
                int R = (int)body_rgb.get(r,c)[0];
                int G = (int)body_rgb.get(r,c)[1];
                int B = (int)body_rgb.get(r,c)[2];

                int H = (int)body_hsv.get(r,c)[0];
                int S = (int)body_hsv.get(r,c)[1];
                int V = (int)body_hsv.get(r,c)[2];

                double var_R = (R / 255.0);
                double var_G = 0;
                double var_B = 0;
                if(G != 0)
                {
                    var_G = (G / 255.0);
                }
                if(B != 0)
                {
                    var_B = (B / 255.0);
                }
                double var_Min = Math.min(var_R,var_G);
                var_Min = Math.min(var_Min,var_B);
                double var_Max = Math.max( var_R, var_G);
                var_Max = Math.max(var_Max,var_G);
                double del_Max = var_Max - var_Min;

                double val_H = 0;
                double val_V = var_Max;

                if ( del_Max == 0 )
                {
                    val_H = 0;
                }
                else
                {
                    double del_R = ( ( ( var_Max - var_R ) / 6 ) + ( del_Max / 2 ) ) / del_Max;
                    double del_G = ( ( ( var_Max - var_G ) / 6 ) + ( del_Max / 2 ) ) / del_Max;
                    double del_B = ( ( ( var_Max - var_B ) / 6 ) + ( del_Max / 2 ) ) / del_Max;
                    if      ( var_R == var_Max ) val_H = del_B - del_G;
                    else if ( var_G == var_Max ) val_H = ( 1 / 3 ) + del_R - del_B;
                    else if ( var_B == var_Max ) val_H = ( 2 / 3 ) + del_G - del_R;
                    if ( val_H < 0 ) val_H += 1;
                    else if ( val_H > 1 ) val_H -= 1;
                }

                if(val_H >= 0.035 && val_H <= 0.09 && val_V > 0.65)
                {
                    body_count++;

                    if(!(body_map_H.containsKey(H))){
                        body_map_H.put(H,1);
                    }
                    else
                    {
                        body_map_H.put(H, body_map_H.get(H) + 1);
                    }

                    if(!(body_map_S.containsKey(S))){
                        body_map_S.put(S,1);
                    }
                    else
                    {
                        body_map_S.put(S, body_map_S.get(S) + 1);
                    }

                    if(!(body_map_V.containsKey(V))){
                        body_map_V.put(V,1);
                    }
                    else
                    {
                        body_map_V.put(V, body_map_V.get(V) + 1);
                    }
                }
                else
                {
                    body_rgb.put(r,c,data);
                }
            }
        }
        //Body end

        Utils.matToBitmap(body_rgb,myBitmap2);
        Map<Integer, Integer> sortedfaceMap_H = sortByFloatValue(face_map_H);
        Map<Integer, Integer> sortedfaceMap_S = sortByFloatValue(face_map_S);
        Map<Integer, Integer> sortedfaceMap_V = sortByFloatValue(face_map_V);
        Map<Integer, Integer> sortedbodyMap_H = sortByFloatValue(body_map_H);
        Map<Integer, Integer> sortedbodyMap_S = sortByFloatValue(body_map_S);
        Map<Integer, Integer> sortedbodyMap_V = sortByFloatValue(body_map_V);

        face_map_H.clear();
        face_map_S.clear();
        face_map_V.clear();
        body_map_H.clear();
        body_map_S.clear();
        body_map_V.clear();

        int faceH_size = 0;
        int bodyH_size = 0;

        System.out.println("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
        sortedfaceMap_H = printMap(sortedfaceMap_H, face_count);
        System.out.println("========================================");
        sortedbodyMap_H = printMap(sortedbodyMap_H, body_count);
        System.out.println("SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS");
        sortedfaceMap_S = printMap(sortedfaceMap_S, face_count);
        System.out.println("========================================");
        sortedbodyMap_S = printMap(sortedbodyMap_S, body_count);
        System.out.println("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
        sortedfaceMap_V = printMap(sortedfaceMap_V, face_count);
        System.out.println("========================================");
        sortedbodyMap_V = printMap(sortedbodyMap_V, body_count);


        int Hrate_q = sortedbodyMap_H.size() / sortedfaceMap_H.size();
        int Srate_q = sortedbodyMap_S.size() / sortedfaceMap_S.size();
        int Vrate_q = sortedbodyMap_V.size() / sortedfaceMap_V.size();

        Map<Integer , Integer> face_invert_H = new HashMap<Integer , Integer>();
        Map<Integer , Integer> body_invert_H = new HashMap<Integer , Integer>();
        Map<Integer , Integer> face_invert_S = new HashMap<Integer , Integer>();
        Map<Integer , Integer> body_invert_S = new HashMap<Integer , Integer>();
        Map<Integer , Integer> face_invert_V = new HashMap<Integer , Integer>();
        Map<Integer , Integer> body_invert_V = new HashMap<Integer , Integer>();
        Map<Integer, Integer> body_temp_H = sortByFloatValue_n(sortedbodyMap_H);
        Map<Integer, Integer> body_temp_S = sortByFloatValue_n(sortedbodyMap_S);
        Map<Integer, Integer> body_temp_V = sortByFloatValue_n(sortedbodyMap_V);

        int value_faceH = 0;
        int value_bodyH = 0;
        int value_faceS = 0;
        int value_bodyS = 0;
        int value_faceV = 0;
        int value_bodyV = 0;


        double aver_faceH = 0.0;
        double aver_bodyH = 0.0;
        double aver_faceS = 0.0;
        double aver_bodyS = 0.0;
        double aver_faceV = 0.0;
        double aver_bodyV = 0.0;
        int count = 0;
        int rank = 0;

        Iterator<Integer> iter = sortedfaceMap_H.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            int value = sortedfaceMap_H.get(key);
            face_invert_H.put(value,key);
            value_faceH = value_faceH + key;
        }

        iter = sortedfaceMap_S.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            int value = sortedfaceMap_S.get(key);
            face_invert_S.put(value,key);
            value_faceS = value_faceS + key;
        }

        iter = sortedfaceMap_V.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            int value = sortedfaceMap_V.get(key);
            face_invert_V.put(value,key);
            value_faceV = value_faceV + key;
        }




        iter = body_temp_H.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            body_invert_H.put(key,rank);
            System.out.println("key : " + key + " Value : " + rank);
            value_bodyH = value_bodyH +  key;
            count++;
            if(count == Hrate_q)
            {
                count = 0;
                rank++;
                if(rank == face_invert_H.size())
                {
                    rank--;
                }
            }
        }

        iter = body_temp_S.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            body_invert_S.put(key,rank);
            System.out.println("key : " + key + " Value : " + rank);
            value_bodyS = value_bodyS +  key;
            count++;
            if(count == Srate_q)
            {
                count = 0;
                rank++;
                if(rank == face_invert_S.size())
                {
                    rank--;
                }
            }
        }

        iter = body_temp_H.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            body_invert_H.put(key,rank);
            System.out.println("key : " + key + " Value : " + rank);
            value_bodyV = value_bodyV +  key;
            count++;
            if(count == Hrate_q)
            {
                count = 0;
                rank++;
                if(rank == face_invert_H.size())
                {
                    rank--;
                }
            }
        }


        iter = body_temp_V.keySet().iterator();
        while(iter.hasNext()) {
            int key = iter.next();
            body_invert_V.put(key,rank);
            System.out.println("key : " + key + " Value : " + rank);
            value_bodyV = value_bodyV +  key;
            count++;
            if(count == Vrate_q)
            {
                count = 0;
                rank++;
                if(rank == face_invert_V.size())
                {
                    rank--;
                }
            }
        }

        aver_faceH = value_faceH / sortedfaceMap_H.size();
        aver_bodyH = value_bodyH / sortedbodyMap_H.size();
        aver_faceS = value_faceS / sortedfaceMap_S.size();
        aver_bodyS = value_bodyS / sortedbodyMap_S.size();
        aver_faceV = value_faceV / sortedfaceMap_V.size();
        aver_bodyV = value_bodyV / sortedbodyMap_V.size();

        System.out.println("aver_faceH : " + aver_faceH);
        System.out.println("aver_faceS : " + aver_faceS);
        System.out.println("aver_faceV : " + aver_faceV);
        System.out.println("aver_bodyH : " + aver_bodyH);
        System.out.println("aver_bodyS : " + aver_bodyS);
        System.out.println("aver_bodyV : " + aver_bodyV);

        aver_faceH = aver_bodyH - aver_faceH;
        aver_faceS = aver_bodyS - aver_faceS;
        aver_faceV = aver_bodyV - aver_faceV;

        System.out.println("aver_faceH : " + aver_faceH);
        System.out.println("aver_faceS : " + aver_faceS);
        System.out.println("aver_faceV : " + aver_faceV);


        double[] HSV = new double[3];

        for(int r = 0; r < body_hsv.rows(); r++)
        {
            for(int c = 0; c < body_hsv.cols(); c++)
            {
                int H = (int)body_hsv.get(r,c)[0];
                int S = (int)body_hsv.get(r,c)[1];
                int V = (int)body_hsv.get(r,c)[2];

                int R = (int)body_rgb.get(r,c)[0];
                int G = (int)body_rgb.get(r,c)[1];
                int B = (int)body_rgb.get(r,c)[2];

                if(!(R == 255 && G == 255 && B == 255))
                {
                    try
                    {
                        double H_new = H - aver_faceH;
                        double S_new = S - aver_faceS;
                        double V_new = V - aver_faceV;
                        HSV[0] = H_new;
                        HSV[1] = S_new;
                        HSV[2] = V_new;
                        body_hsv.put(r,c,HSV);
                    }
                    catch (NullPointerException e)
                    {

                    }
                }
            }
        }

        Imgproc.cvtColor(body_hsv,body_rgb,Imgproc.COLOR_HSV2BGR);
        Utils.matToBitmap(body_rgb,myBitmap2);

    }

    public static <K, V> Map printMap(Map<K, V> map, int count) {

        double coun = count;
        double rate_sum = 0;
        int rank = 0;
        Map<Integer , Integer> map1 = new HashMap<Integer , Integer>();

        for (Map.Entry<K, V> entry : map.entrySet()) {

            double rate = Integer.valueOf(entry.getValue().toString());
            //System.out.println("Key : " + entry.getKey() + " rate : " + rate / coun);
            map1.put(Integer.valueOf(entry.getKey().toString()), rank);
            System.out.println("Key : " + entry.getKey() + " Value : " + map1.get(entry.getKey()).intValue());
            rank++;
            rate_sum = rate_sum + (rate / coun);
            if(rate_sum > 0.9)
            {
                break;
            }

        }
        return map1;
    }


    private static Map<Integer, Integer> sortByFloatValue(Map<Integer, Integer> unsortMap) {
        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(unsortMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return -1 * ((o1.getValue()).compareTo(o2.getValue()));
            }
        });

        /// Loop the sorted list and put it into a new insertion order Map
        /// LinkedHashMap

        Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private static Map<Integer, Integer> sortByFloatValue_n(Map<Integer, Integer> unsortMap) {
        List<Map.Entry<Integer, Integer>> list = new LinkedList<Map.Entry<Integer, Integer>>(unsortMap.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Integer>>() {
            public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
                return  (o1.getValue()).compareTo(o2.getValue());
            }
        });

        /// Loop the sorted list and put it into a new insertion order Map
        /// LinkedHashMap

        Map<Integer, Integer> sortedMap = new LinkedHashMap<Integer, Integer>();
        for (Map.Entry<Integer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private void face_dectect(View v)
    {

        System.out.println("ho");
        float x1 = 0,x2 = 0,y1 = 0,y2 = 0;
        float x1_2 = 0,x2_2 = 0,y1_2 = 0,y2_2 = 0;
        float mouse_x = 0.0f;
        float mouse_y = 0.0f;
        float nose_x = 0.0f;
        float nose_y = 0.0f;
        float cheek_leftX = 0.0f;
        float cheek_leftY = 0.0f;
        float cheek_rightX = 0.0f;
        float cheek_rightY = 0.0f;

        float left_x = 0.0f;
        float left_y = 0.0f;
        float right_x = 0.0f;
        float right_y = 0.0f;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap myBitmap = upload_bm;
        Mat body = new Mat();
        try
        {
            Utils.bitmapToMat(upload_bm2,body);
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        //Rect Point color
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        //circle color paint
        Paint Pnt = new Paint();
        Pnt.setColor(Color.GREEN);
        Pnt.setStrokeWidth(5);
        Pnt.setStyle(Paint.Style.STROKE);

        Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //얼굴 인식 부분

        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
            .setTrackingEnabled(false)
            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
            .build();
        if(!faceDetector.isOperational()){
            new AlertDialog.Builder(v.getContext()).setMessage("얼굴 검출 실패(얼굴 없음)");
            return;
        }

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = null;
        try
        {
            faces = faceDetector.detect(frame);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        if(faces.size() == 0)
        {
            System.out.println("얼굴 없는 이미지");
            return;
        }
        else if(faces.size() > 1)
        {
            System.out.println("얼굴 많음");
            return;
        }

        Face thisFace = faces.valueAt(0);
        x1 = thisFace.getPosition().x;
        y1 = thisFace.getPosition().y;
        x2 = x1 + thisFace.getWidth();
        y2 = y1 + thisFace.getHeight();
        if(x1 < 0)
        {
            x1 = 0;
        }
        if(y1 < 0)
        {
            y1 = 0;
        }
        tempCanvas.drawRoundRect(new RectF(x1,y1,x2,y2), 2, 2, myRectPaint);

        //draw landmarks
        List<Landmark> landmarks = thisFace.getLandmarks();
        //for make landmark circle
        Landmark thisLand = landmarks.get(2);
        nose_x = thisLand.getPosition().x;
        nose_y = thisLand.getPosition().y;
        tempCanvas.drawCircle(nose_x, nose_y, 10, Pnt);

        float yy1;
        float jj;

        thisLand = landmarks.get(0);
        yy1 = thisLand.getPosition().y;

        thisLand = landmarks.get(4);
        left_x = thisLand.getPosition().x;
        left_y = thisLand.getPosition().y;
        tempCanvas.drawCircle(mouse_x, left_y, 10, Pnt);

        jj = left_y - yy1;
        yy1 = yy1-jj;
        tempCanvas.drawCircle(mouse_x, yy1, 3, Pnt);

        thisLand = landmarks.get(3);
        right_x = thisLand.getPosition().x;
        right_y = thisLand.getPosition().y;
        tempCanvas.drawCircle(mouse_x, mouse_y, 10, Pnt);

        thisLand = landmarks.get(7);
        mouse_x = thisLand.getPosition().x;
        mouse_y = thisLand.getPosition().y;
        tempCanvas.drawCircle(mouse_x, mouse_y, 10, Pnt);

        Mat face1_rgb = new Mat();
        Mat face2_rgb = new Mat();
        Utils.bitmapToMat(upload_bm,face1_rgb);

        double distance_x = thisFace.getWidth() - x1;
        double thisgetwidth = thisFace.getWidth();
        int dis_int = (int)(distance_x * 0.23);
        double xk = x1 + dis_int;

        double y_location = thisFace.getHeight() + y1;
        double thisgetheight = y_location - yy1;
        thisgetheight = thisgetheight * 0.95;
        thisgetwidth = thisgetwidth - (dis_int * 2);

        myBitmap =  Bitmap.createBitmap((int)face1_rgb.size().width,(int)face1_rgb.size().height, Bitmap.Config.ARGB_8888);
        Bitmap dstBmp = Bitmap.createBitmap(tempBitmap,(int)xk,(int)yy1,(int)thisgetwidth,(int)thisgetheight);
        face1_rgb = grabcut(face1_rgb, (int)xk, (int)yy1, (int)thisgetwidth, (int)thisgetheight, 3);

        double[] change_RGB = new double[3];
        int chin_x = 0;
        int chin_y = 0;
        for(int c = (int)mouse_y; c < face1_rgb.height(); c++) {
            change_RGB[0] = face1_rgb.get(c, (int) mouse_x)[0];
            change_RGB[1] = face1_rgb.get(c, (int) mouse_x)[1];
            change_RGB[2] = face1_rgb.get(c, (int) mouse_x)[2];

            if (change_RGB[0] == 0 && change_RGB[1] == 0 && change_RGB[2] == 0) {
                chin_x = (int)mouse_x;
                chin_y = c;
                break;
            }

        }

        for(int c = (int)left_x; c > 0; c--) {
            change_RGB[0] = face1_rgb.get((int)left_y, c)[0];
            change_RGB[1] = face1_rgb.get((int)left_y, c)[1];
            change_RGB[2] = face1_rgb.get((int)left_y, c)[2];

            if (change_RGB[0] == 0 && change_RGB[1] == 0 && change_RGB[2] == 0) {
                cheek_leftX = c;
                cheek_leftY = left_y;
                break;
            }

        }

        for(int c = (int)right_x; c < face1_rgb.width(); c++) {
            change_RGB[0] = face1_rgb.get((int)right_y, c)[0];
            change_RGB[1] = face1_rgb.get((int)right_y, c)[1];
            change_RGB[2] = face1_rgb.get((int)right_y, c)[2];

            if (change_RGB[0] == 0 && change_RGB[1] == 0 && change_RGB[2] == 0) {
                cheek_rightX = c;
                cheek_rightY = cheek_leftY;
                break;
            }

        }


        Imgproc.Canny(face1_rgb,face1_rgb,100,200);
        //Imgproc.dilate(face1_rgb, face1_rgb, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
        Utils.bitmapToMat(dstBmp,face2_rgb);

        user_face2.setImageDrawable(new BitmapDrawable(getResources(), dstBmp));

        int width_a = (int)(cheek_rightX - cheek_leftX);
        int heigt_a = (int)(chin_y - cheek_leftY);

        int iter1 = chin_x - (int)cheek_leftX;
        int iter2 = (int)cheek_rightX - chin_x;
        double rate_1 = iter1 / (chin_y - cheek_leftY);
        double rate_2 = iter2 / (chin_y - cheek_leftY);
        double[] list_cheekLeft = new double[chin_y - (int)cheek_leftY];
        double[] list_cheekRight = new double[chin_y - (int)cheek_leftY];

        list_cheekLeft[0] = cheek_leftX;
        list_cheekRight[0] = cheek_rightX;

        //시작 2017

        for(int i = 1; i < list_cheekLeft.length; i++)
        {
            list_cheekLeft[i] = list_cheekLeft[i-1] + rate_1;
        }
        for(int i = 1; i < list_cheekRight.length; i++)
        {
            list_cheekRight[i] = list_cheekRight[i-1] - rate_2;
        }

        int count = 0;
        for(int i = (int)cheek_leftY; i < chin_y; i++)
        {
            for(int j = (int)cheek_leftX; j <= chin_x; j++)
            {
                if(j > list_cheekLeft[count])
                {
                    face1_rgb.put(i, j, 0);
                }
            }
            count++;
        }

        count = 0;
        for(int i = (int)cheek_leftY; i < chin_y; i++)
        {
            for(int j = (int)cheek_rightX; j > chin_x; j--)
            {
                if(j <= list_cheekRight[count])
                {
                    face1_rgb.put(i, j, 0);
                }
            }
            count++;
        }

        for(int i = 0; i < face1_rgb.height(); i++)
        {
            for(int j = 0; j < face1_rgb.width(); j++)
            {
                if(i < (int)cheek_leftY)
                {
                    face1_rgb.put(i, j, 0);
                }
                else if(i == (int)cheek_leftY)
                {
                    i = chin_y;
                }
                else
                {
                    face1_rgb.put(i, j, 0);
                }
            }
        }

        int[] leftcount = new int[chin_y - (int)cheek_leftY];
        int[] rightcount = new int[chin_y - (int)cheek_leftY];
        int remember_x = 0;
        count = 0;
        for(int i = (int)cheek_leftY; i < chin_y; i++)
        {
            for(int j = 0; j <= chin_x; j++)
            {
                double ch = face1_rgb.get(i,j)[0];
                if(ch == 255)
                {
                    count++;
                    face1_rgb.put(i, j, 0);
                    remember_x = j;
                }
            }
            if(count != 0)
            {
                face1_rgb.put(i, remember_x, 255);
                leftcount[i - (int)cheek_leftY] = remember_x;
            }
            else
            {
                leftcount[i - (int)cheek_leftY] = 0;
            }
            count = 0;
        }


        remember_x = 0;
        count = 0;
        for(int i = (int)cheek_leftY; i < chin_y; i++)
        {
            for(int j = face1_rgb.width()-1; j >= chin_x; j--)
            {
                double ch = face1_rgb.get(i,j)[0];
                if(ch == 255)
                {
                    count++;
                    face1_rgb.put(i, j, 0);
                    remember_x = j;
                }
            }
            if(count != 0)
            {
                face1_rgb.put(i, remember_x, 255);
                rightcount[i - (int)cheek_leftY] = remember_x;
            }
            else
            {
                rightcount[i - (int)cheek_leftY] = 0;
            }
            count = 0;
        }

        int remember_count = 0;
        for(int i = 1; i < rightcount.length; i++)
        {
            if(rightcount[i] != 0 && rightcount[i-1] != 0)
            {
                System.out.println(rightcount[i] + " - " + rightcount[i-1]);
            }
        }


        Imgproc.dilate(face1_rgb, face1_rgb, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
        Imgproc.erode(face1_rgb, face1_rgb, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1,1)));


        Utils.matToBitmap(face1_rgb,myBitmap);
        //myBitmap = Bitmap.createBitmap(myBitmap,(int)cheek_leftX,(int)cheek_leftY,width_a,heigt_a);
        user_face.setImageDrawable(new BitmapDrawable(getResources(), myBitmap));

        /*
        double[] change_RGB = new double[3];
        double[] cccc = new double[3];
        cccc[0] = 255;
        cccc[1] = 255;
        cccc[2] = 255;

        double[] cccc2 = new double[3];
        cccc[0] = 255;
        cccc[1] = 0;
        cccc[2] = 0;

        int chin_x = 0;
        int chin_y = 0;
        Utils.bitmapToMat(myBitmap, face1_rgb);
        Imgproc.cvtColor(face1_rgb,face1_rgb,Imgproc.COLOR_BGRA2BGR);


        Utils.matToBitmap(face1_rgb, myBitmap );

        System.out.println("face2 : " + dstBmp.getHeight());

        for(int c = (int)mouse_y; c < face1_rgb.height(); c++) {
            change_RGB[0] = face1_rgb.get(c, (int) mouse_x)[0];
            change_RGB[1] = face1_rgb.get(c, (int) mouse_x)[1];
            change_RGB[2] = face1_rgb.get(c, (int) mouse_x)[2];

            if (change_RGB[0] == 0 && change_RGB[1] == 0 && change_RGB[2] == 0) {
                chin_x = (int)mouse_x;
                chin_y = c;
                break;
            }

        }

        System.out.println("y1 : " + y1);
        chin_y = chin_y - (int)y1;
        System.out.println("chin_y : " + chin_y);

        dstBmp = Bitmap.createBitmap(dstBmp, 0 ,0 , dstBmp.getWidth() ,chin_y);
        Utils.bitmapToMat(dstBmp, face2_rgb);
        int x = (int)(face2_rgb.width() * 0.1);
        int y = (int)(face2_rgb.height() * 0.1);
        int x_1 = (int)(face2_rgb.width() * 0.90);

        face2_rgb = grabcut(face2_rgb, x, y, x_1, chin_y, 10);

        int max_x = 0;
        int min_x = 10000000;
        int min_y = 10000000;

        for(int r = 0; r < face2_rgb.width(); r++)
        {
            for(int c = 0; c < face2_rgb.height(); c++)
            {
                change_RGB[0] = face2_rgb.get(c, r)[0];
                change_RGB[1] = face2_rgb.get(c, r)[1];
                change_RGB[2] = face2_rgb.get(c, r)[2];
                if (change_RGB[0] != 0 && change_RGB[1] != 0 && change_RGB[2] != 0) {
                    face2_rgb.put(c,r,cccc);
                    if(r > max_x)
                    {
                        face2_rgb.put(c,r,cccc2);
                        max_x = r;
                    }
                    if(r < min_x)
                    {
                        face2_rgb.put(c,r,cccc2);
                        min_x = r;
                    }
                    if(c < min_y)
                    {
                        face2_rgb.put(c,r,cccc2);
                        min_y = c;
                    }
                    break;
                }
            }
        }

        Utils.matToBitmap(face2_rgb,dstBmp);
        System.out.println("max_x : "+ max_x );
        System.out.println("min_x : "+ min_x );
        System.out.println("min_y : "+ min_y );

        //dstBmp = Bitmap.createBitmap(dstBmp,min_x,min_y ,max_x-min_x, dstBmp.getHeight() - min_y);
        user_face.setImageDrawable(new BitmapDrawable(getResources(), myBitmap));



        Bitmap dstBmp = Bitmap.createBitmap(tempBitmap,(int)x1,(int)y1,(int)thisFace.getWidth(),(int)thisFace.getHeight());

        Mat facemat = new Mat();
        Mat facemat_temp = new Mat();
        Utils.bitmapToMat(upload_bm,facemat);

        System.out.println("facemat : " + facemat.width());
        System.out.println("facemat : " + facemat.height());

        facemat_temp = grabcut(facemat);

        Bitmap dstBmp2 =  Bitmap.createBitmap(facemat_temp.width(), facemat_temp.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(facemat_temp,dstBmp2);
        user_face2.setImageDrawable(new BitmapDrawable(getResources(), dstBmp2));


        System.out.println("face temp : " + facemat_temp.width());
        System.out.println("face temp : " + facemat_temp.height());


        double change_sizeW = facemat_temp.width()/102;
        double change_sizeH = facemat_temp.height()/112;
        Size chage_size = new Size(facemat.width()/change_sizeW, facemat.height()/change_sizeH);
        Size ch_size = new Size(102, 112);

        Imgproc.resize(facemat_temp,facemat_temp,ch_size);
        Imgproc.resize(facemat,facemat,chage_size);

        dstBmp = Bitmap.createBitmap(facemat.width(), facemat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(facemat,dstBmp);
        user_face2.setImageDrawable(new BitmapDrawable(getResources(), dstBmp));


        //70, 8

        double[] change_RGB = new double[3];

        try {
            Imgproc.cvtColor(body,body,Imgproc.COLOR_BGRA2BGR);
        }catch (Exception e)
        {
            e.printStackTrace();
        }

        for(int r = 0; r < facemat_temp.rows(); r++)
        {
            for(int c = 0; c < facemat_temp.cols(); c++)
            {
                change_RGB[0] = facemat_temp.get(r,c)[0];
                change_RGB[1] = facemat_temp.get(r,c)[1];
                change_RGB[2] = facemat_temp.get(r,c)[2];

                if(!(change_RGB[0] == 0 && change_RGB[1] == 0 && change_RGB[2] == 0))
                {
                    body.put(8+r,70+c, change_RGB);
                }
            }
        }

        dstBmp2 =  Bitmap.createBitmap(body.width(), body.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(body,dstBmp2);
        user_face2.setImageDrawable(new BitmapDrawable(getResources(), dstBmp2));

        //Imgproc.resize(face1_rgb,face1_rgb,face2_rgb.size());
        //Utils.matToBitmap(face1_rgb,dstBmp2);

        faceDetector.release();

*/
}

    private Mat grabcut(Mat sample, int x, int y, int endx, int endy, int iter)
    {
        Imgproc.cvtColor(sample,sample,Imgproc.COLOR_BGRA2BGR);
        Mat g_mask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();
        Rect rect = new Rect(x, y, endx,endy);
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3));
        Imgproc.grabCut(sample, g_mask, rect, bgModel, fgModel, iter, Imgproc.GC_INIT_WITH_RECT);
        Core.compare(g_mask, source, g_mask, Core.CMP_EQ);
        Mat fg = new Mat(sample.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
        sample.copyTo(fg, g_mask);
        return fg;
    }


    private File getImageFile(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        if (uri == null) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        Cursor mCursor = getContentResolver().query(uri, projection, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " desc");
        if(mCursor == null || mCursor.getCount() < 1) {
            return null; // no cursor or no record
        }
        int column_index = mCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        mCursor.moveToFirst();

        String path = mCursor.getString(column_index);

        if (mCursor !=null ) {
            mCursor.close();
            mCursor = null;
        }

        return new File(path);
    }

    public void TakePicture() {

        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        //임시로 사용할 파일의 경로
        String url = "tmp_" + String.valueOf(System.currentTimeMillis()) + ".jpg";
        CaptureUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), url));
        //인텐트를 이용
        intent.putExtra(MediaStore.EXTRA_OUTPUT, CaptureUri);
        startActivityForResult(intent, PICK_FROM_CAMERA);

    }

    // 앨범으로 부터 사진을 추출
    public void Album() {

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent,PICK_FROM_ALBUM);

    }
}
