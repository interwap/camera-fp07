package com.praescient.components.camera_fp07;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.praescient.components.camera_fp07.utils.FocusBox;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class CameraFP07 extends Activity implements View.OnClickListener, SurfaceHolder.Callback {

    private static final int CAMERA_REQUEST_CODE = 1;
    private Context context = CameraFP07.this;

    private Camera camera;
    private RelativeLayout relativeLayout;
    private FrameLayout frameLayout;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private FocusBox focusBox;
    private Camera.PictureCallback raw;
    private Camera.ShutterCallback capture;
    private Camera.PictureCallback jpeg;
    private Camera.Parameters parameters;
    private int rotation;
    private int angle;
    private Bundle extras;
    private int quality = 70;
    public String storage;

    //Buttons
    private ImageButton shutter;
    public ImageButton flash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Go fullscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        shutter = (ImageButton) findViewById(R.id.shutter);
        shutter.setOnClickListener(this);

        ImageButton close = (ImageButton) findViewById(R.id.close);
        close.setOnClickListener(this);

        flash = (ImageButton) findViewById(R.id.flash);
        flash.setOnClickListener(this);
        flash.setTag("unclicked");

        ImageButton focus = (ImageButton) findViewById(R.id.focus);
        focus.setOnClickListener(this);

        shutter = (ImageButton) findViewById(R.id.shutter);
        shutter.setOnClickListener(this);

        extras = getIntent().getExtras();
        if (extras != null) {

            quality = extras.getInt("quality");
            storage = extras.getString("storage").trim();

        }
        launchCamera();

        callbacks();

    }

    //Launch Camera UI
    public void launchCamera(){

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        relativeLayout = (RelativeLayout) findViewById(R.id.frameParent);
        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    //Open Camera
    private void openCamera(){

        releaseCamera();

        try {

            camera = Camera.open(0);

        } catch (Exception e){
            e.printStackTrace();
        }

        if (camera != null) {

            try {

                setupCamera(0);

                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();

            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    // Setup Camera
    public void setupCamera(int id){

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        rotation = getWindowManager().getDefaultDisplay().getRotation();
        angle = 0;

        switch (rotation) {

            case Surface.ROTATION_0:
                angle = 0;
                break;

            case Surface.ROTATION_90:
                angle = 90;
                break;

            case Surface.ROTATION_180:
                angle = 180;
                break;

            case Surface.ROTATION_270:
                angle = 270;
                break;

            default:
                break;
        }

        rotation = (info.orientation - angle + 360) % 360;

        camera.setDisplayOrientation(rotation);
        Camera.Size previewSize = getOptimalPreviewSize(camera.getParameters().getSupportedPreviewSizes(),  display.getWidth(), display.getHeight());

        parameters = camera.getParameters();
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setZoom(0);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.set("jpeg-quality", 100);

        camera.setParameters(parameters);

    }

    // Release Camera Resources
    private void releaseCamera(){

        try {

            if(camera != null){

                camera.setPreviewCallback(null);
                camera.setErrorCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        openCamera();

    }

    @Override
    public void onClick(View view) {

        int i = view.getId();

        //Handle Flash Event
        if (i == R.id.flash) {

            toggleFlash();

        }

        //Handle Close Event
        if (i == R.id.focus) {
            tryAutoFocus();
        }

        //Handle Close Event
        if (i == R.id.close) {
            onBackPressed();
        }

        //Handle Shutter Event
        if (i == R.id.shutter) {
            takePhoto();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    //Try AutoFocus
    public void tryAutoFocus(){

        if(camera == null || surfaceHolder.getSurface() == null){
            Log.i("Log", "No Camera or Preview");

        }else {

            Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    Log.i("Log", "Auto Focus Complete");
                }
            };
            camera.autoFocus(autoFocusCallback);
        }
    }

    // Activate Camera Flash
    public void toggleFlash() {

        if(flash.getTag() == "unclicked"){

            flash.setTag("clicked");
            flash.setImageResource(R.drawable.flash);
            Toast.makeText(context, "Flash On", Toast.LENGTH_SHORT).show();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            camera.setParameters(parameters);

        } else {

            flash.setTag("unclicked");
            flash.setImageResource(R.drawable.flash_off);
            Toast.makeText(context, "Flash Off", Toast.LENGTH_SHORT).show();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);

        }
    }

    // Get Optimal Preview Size
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizeList, int w, int h){

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h/w;

        if(sizeList == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizeList) {

            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff){
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizeList) {
                if (Math.abs (size.height - h) < minDiff){
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }
        return optimalSize;
    }

    //Take Photo
    private void takePhoto(){
        camera.takePicture(capture, raw, jpeg);
    }

    private void callbacks(){

        capture = new Camera.ShutterCallback(){
            @Override
            public void onShutter() {
                AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
            }
        };

        jpeg = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {

                Log.i("Photo", "The Picture taken");

                if(bytes == null){

                    Log.i("Photo", "Bytes Not Received");

                    return;

                } else {
                    Log.i("Photo", "Bytes Received");
                }

                File newMedia = null;

//                try {
//                    newMedia = createImageFile();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferQualityOverSpeed = true;
                options.inScaled = false;
                options.inDither = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap decodePhoto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

                Matrix matrix = new Matrix();
                matrix.postRotate(270);

                int sides = 1040;
                int width = decodePhoto.getWidth();
                int height = decodePhoto.getHeight();
                int left = (width - sides)/2;
                int top = (height - sides)/2;
                int right = left + sides;
                int bottom = top + sides;

                Bitmap newPhoto = Bitmap.createBitmap(decodePhoto, left, top, sides, sides, matrix, false);
                //Bitmap newPhoto = Bitmap.createBitmap(decodePhoto, left, top, sides, sides);

                Intent result = new Intent();
                result.putExtra("imageURI", saveCroppedPhoto(newPhoto));
                setResult(RESULT_OK, result);
                Toast.makeText(getApplicationContext(), "Photo Saved" ,Toast.LENGTH_LONG).show();
                //openCamera();
                releaseCamera();
                finish();

            }
        };

    }

    //Create Actual Image File
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFile = "IMAGE_" + timeStamp + "_";
        File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFile,".jpg", storageDirectory);
        return image;

    }

    // Algorithm to crop captured photo from center
    // Return path
    public String saveCroppedPhoto(Bitmap bitmap){

        String root = Environment.getExternalStorageDirectory().toString();
        String imageURI = null;
        File myDir;

        if (storage != null && !storage.isEmpty() && !storage.equals("null")){
            myDir   = new File(root + File.separator + storage);
        } else {
            myDir = new File(root);
        }

        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-" + n + ".jpg";
        File file = new File(myDir, fname);
        Log.i("Photo", "" + file);
        if (file.exists()){
            file.delete();
        } else{
            try {

                if(quality == 0){ quality = 70;}

                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                imageURI = file.getAbsolutePath();
                //imageURI = file.getName();
                out.flush();
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return imageURI;
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();

        openCamera();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        releaseCamera();
        finish();
    }

}
