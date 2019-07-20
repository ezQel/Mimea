package com.ezqel.mimea;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.Inflater;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.Response;

import android.support.design.widget.BottomSheetBehavior;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {


    private static LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior sheetBehavior;



    private Camera mCamera;
    private static CameraPreview mPreview;
    protected ImageView bottomSheetArrowImageView;
    static ImageButton captureButton, retakeButton;
    TextView english_name, latin_name, desc, name_display;
    static ImageView imageView;
    static ImageButton go_btn;
    static FrameLayout preview;
    static LoadDialog dialog;

    byte[] image_data;
    private static Call<JsonObject> cl;

    String url = "http://192.168.217.1:5000/upload.jpg";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getSupportActionBar().hide();

        imageView = findViewById(R.id.plant_image);
        go_btn = findViewById(R.id.go);
        captureButton = findViewById(R.id.button_capture);
        retakeButton = findViewById(R.id.retake);
        english_name = findViewById(R.id.english_name);
        latin_name = findViewById(R.id.latin_name);
        name_display = findViewById(R.id.display_Name);
        desc = findViewById(R.id.description);

        dialog= new LoadDialog();



        // Create an instance of Camera
        mCamera = getCameraInstance();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setRotation(90);
        mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);


        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
        gestureLayout = findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

        bottomSheetLayout.setVisibility(View.INVISIBLE);
        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        //                int width = bottomSheetLayout.getMeasuredWidth();
                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED:
                            {
                                bottomSheetArrowImageView.setImageResource(R.drawable.ic_chevron_down);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED:
                            {
                                bottomSheetArrowImageView.setImageResource(R.drawable.ic_chevron_up);
                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.ic_chevron_up);
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
                });




    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mCamera.stopPreview();
        mCamera.release();

    }


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            try{
                /*
                //rotate image
                Bitmap bitmap = (Bitmap)BitmapFactory.decodeByteArray(data,0,data.length);
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                */
                //upload image
                image_data=data;

            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

    };

    public void clicked(View v) {
        // get an image from the camera
        mCamera.takePicture(null, null, mPicture);
        captureButton.setVisibility(View.INVISIBLE);
        go_btn.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.VISIBLE);

        //bottomSheetLayout.setVisibility(View.VISIBLE);
    }

    public void retake(View view){
        preview.removeAllViews();
        preview.addView(mPreview);
        captureButton.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.INVISIBLE);
        go_btn.setVisibility(View.INVISIBLE);
        bottomSheetLayout.setVisibility(View.INVISIBLE);
    }

    public void go(View v){
        retakeButton.setVisibility(View.INVISIBLE);
        go_btn.setVisibility(View.INVISIBLE);
        uploadToServer(image_data);
        dialog.show(getSupportFragmentManager(),"loading");
        dialog.setCancelable(false);
    }


    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)

        }
        return c; // returns null if camera is unavailable
    }



    private void uploadToServer(byte[] file) {
        Retrofit retrofit = NetworkClient.getRetrofitClient(this);
        UploadAPIs uploadAPIs = retrofit.create(UploadAPIs.class);
        //Create a unique filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename="IMG_"+ timeStamp + ".jpg";
        //File file = file;
        // Create a request body with file and image media type
        RequestBody fileReqBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        // Create MultipartBody.Part using file request-body,file name and part name
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, fileReqBody);
        //Create request body with text description and text media type
        final RequestBody description = RequestBody.create(MediaType.parse("multipart/form-data"), "image/*");
        //
        cl = uploadAPIs.uploadImage(part, description);
        cl.enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                new DownloadWebpageTask().execute(url);
                try {
                    JSONObject jsonObject = new JSONObject(new Gson().toJson(response.body()));
                    String englishName=jsonObject.getString("english_name");
                    String latinName=jsonObject.getString("latin_name");
                    String desC=jsonObject.getString("description");

                    name_display.setText(englishName);
                    english_name.setText(englishName);
                    latin_name.setText(latinName);
                    desc.setText(desC);

                }
                catch (Exception e){
                    e.printStackTrace();
                }
                bottomSheetLayout.setVisibility(View.VISIBLE);
                retakeButton.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }

            @Override
            public void onFailure(Call call, Throwable t) {
                //dialogBox "NETWORK ERROR
                if(cl.isCanceled()){
                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(),"TASK CANCELLED", Toast.LENGTH_SHORT).show();
                }
                else{
                    dialog.dismiss();

                    NetworkUnavailable networkUnavailable=new NetworkUnavailable();
                    networkUnavailable.show(getSupportFragmentManager(),"timeout or no network");
                   // networkUnavailable.setCancelable(false);
                    retakeButton.setVisibility(View.VISIBLE);
                    go_btn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private class DownloadWebpageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return null;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(Bitmap result) {

            imageView.setImageBitmap(result);
            preview.removeAllViews();
            preview.addView(imageView);

        }
    }


    public Bitmap readIt(InputStream stream) throws IOException {

        Bitmap bitmap = BitmapFactory.decodeStream(stream);

        return bitmap;
    }


    public static final String DEBUG_TAG="AsyncNetwork";
    private Bitmap downloadUrl(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            Bitmap bitmap = readIt(is);
            return bitmap;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }


    public static class LoadDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.dialog_loading, null))
                // Add action buttons
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        preview.removeAllViews();
                        preview.addView(mPreview);
                        captureButton.setVisibility(View.VISIBLE);
                        retakeButton.setVisibility(View.INVISIBLE);
                        go_btn.setVisibility(View.INVISIBLE);
                        bottomSheetLayout.setVisibility(View.INVISIBLE);
                        LoadDialog.this.getDialog().cancel();
                        cl.cancel();
                    }
                }).setCancelable(false);
            return builder.create();
        }
}


    public static class NetworkUnavailable extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(inflater.inflate(R.layout.network_error, null))
                    // Add action buttons
                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            retakeButton.setVisibility(View.VISIBLE);
                            go_btn.setVisibility(View.VISIBLE);
                            bottomSheetLayout.setVisibility(View.INVISIBLE);
                            NetworkUnavailable.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }
    }


 }

