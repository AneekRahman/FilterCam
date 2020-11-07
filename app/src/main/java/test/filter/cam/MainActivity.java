package test.filter.cam;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends AppCompatActivity {

    //---------------------------------------------------------------------------------------------

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();
    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    FrameLayout previewHolder;
    ImageButton captureBtn, switchCamBtn, backBtn, redoBtn, checkBtn;
    TextView errorView;
    Button grantPermBtn, saveBtn;
    ImageView imagePostViewIV;
    RelativeLayout postViewLayout, captureLayout;

    private TextureView mTextureView;
    private CameraRenderer renderer;
    String mediaFileNameTemp;
    boolean videoMode = true;
    int mCameraId;
    byte[] mCurrentBitmapData;
    boolean imageSaved = false;
    Camera mCamera;


    private String APP_NAME;

    //---------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------------------------------------------------------------------------------------------

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //---------------------------------------------------------------------------------------------

        previewHolder = (FrameLayout) findViewById(R.id.camera_preview);
        captureBtn = (ImageButton) findViewById(R.id.capture_btn);
        errorView = (TextView) findViewById(R.id.cam_error_view);
        grantPermBtn = (Button) findViewById(R.id.grant_permission_btn);
        imagePostViewIV = (ImageView) findViewById(R.id.image_postview_IV);
        switchCamBtn = (ImageButton) findViewById(R.id.switch_cam_btn);
        postViewLayout = (RelativeLayout) findViewById(R.id.postview_layout);
        captureLayout = (RelativeLayout) findViewById(R.id.capture_screen_layout);
        backBtn = (ImageButton) findViewById(R.id.back_btn);
        redoBtn = (ImageButton) findViewById(R.id.redo_btn);
        checkBtn = (ImageButton) findViewById(R.id.check_btn);
        saveBtn = (Button) findViewById(R.id.save_btn);

        APP_NAME = getString(R.string.app_name);

        //---------------------------------------------------------------------------------------------

        checkPermsAndSetupCam();

        //---------------------------------------------------------------------------------------------

        grantPermBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String[] permissionRequests = {
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                };
                int GRANTED = PackageManager.PERMISSION_GRANTED;

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != GRANTED
                        || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != GRANTED
                        || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != GRANTED) {

                    errorView.setText("Camera, microphone and storage permission not granted");
                    ActivityCompat.requestPermissions(MainActivity.this, permissionRequests, 2);

                }else{

                    checkPermsAndSetupCam();

                }

            }
        });

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(renderer != null){



                }

            }
        });

        switchCamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //toggleCamera();

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();

            }
        });

        redoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(mTextureView != null){

                    mTextureView.setSurfaceTextureListener(null);
                    mTextureView.setSurfaceTextureListener(renderer);
                    renderer.setSelectedFilter(R.id.filter8);

                }

            }
        });

        //---------------------------------------------------------------------------------------------

    }

    // ------------------------------  ALL CAMERA INITIALIZATION STUFF BELOW ---------------------------------

    private void initializeCam(){

        errorView.setVisibility(View.GONE);
        grantPermBtn.setVisibility(View.GONE);
        captureLayout.setVisibility(View.VISIBLE);


        renderer = new CameraRenderer(this);
        mTextureView = new TextureView(this);
        previewHolder.addView(mTextureView);
        mTextureView.setSurfaceTextureListener(renderer);
        renderer.setSelectedFilter(R.id.filter8);

    }

    private boolean capture() {

        File imageFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        if (imageFile.exists()) {
            imageFile.delete();
        }

        // create bitmap screen capture
        Bitmap bitmap = mTextureView.getBitmap();
        startImagePostView(bitmap);
        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(getApplicationContext(), "SAVED", Toast.LENGTH_LONG).show();

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(imageFile);
            mediaScanIntent.setData(contentUri);
            sendBroadcast(mediaScanIntent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }



/*

    public void toggleCamera() {
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                cameraFront = false;
                mCameraId = findBackFacingCamera();
                releaseCameraAndPreview();
                mCamera = Camera.open(cameraId);
                specUpCamera(mCamera);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
                switchCamBtn.setImageResource(R.drawable.ic_phone_front_rotate_icon);
                if(mediaFileNameTemp != null) startVideoPostView();
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the FrontFacingCamera
                cameraFront = true;
                mCameraId = findFrontFacingCamera();
                releaseCameraAndPreview();
                mCamera = Camera.open(cameraId);
                specUpCamera(mCamera);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
                switchCamBtn.setImageResource(R.drawable.ic_phone_rear_rotate_icon);
                if(mediaFileNameTemp != null) startVideoPostView();
            }
        }
    }

*/

    private File getOutputMediaFile(int MEDIA_TYPE) {

        if(Environment.getExternalStorageState() == Environment.MEDIA_UNMOUNTED){
            Toast.makeText(getApplicationContext(), "Cannot access storage", Toast.LENGTH_SHORT).show();
            return null;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
        String date = dateFormat.format(new Date());

        if(MEDIA_TYPE == MEDIA_TYPE_IMAGE){

            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_NAME);
            File internalStorageDir = new File(getFilesDir(), APP_NAME);

            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                mediaStorageDir = internalStorageDir;
            }else if(!internalStorageDir.exists() && !internalStorageDir.mkdirs()){
                Log.d("CameraActivity", "Can't create directory to save image.");
                Toast.makeText(this, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
            }
            String photoFile = "IMG_"  + date + ".jpg";

            String filename = mediaStorageDir.getPath() + File.separator + photoFile;
            File pictureFile = new File(filename);

            return pictureFile;

        }else if(MEDIA_TYPE == MEDIA_TYPE_VIDEO){

            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), APP_NAME);

            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d("CameraActivity", "Can't create directory to save image.");
                Toast.makeText(this, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
            }
            String videoFile = "VID_"  + date + ".mp4";

            String filename = mediaStorageDir.getPath() + File.separator + videoFile;
            mediaFileNameTemp = filename; // To access it from video preview function

            File movieFile = new File(filename);

            return movieFile;

        }else return null;

    }

    public void checkPermsAndSetupCam(){
        /* Check if this device has a camera */

        if (hasCamera()){
            //  Device has a camera

            String[] permissionRequests = {
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
            };
            int GRANTED = PackageManager.PERMISSION_GRANTED;

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != GRANTED
                    || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != GRANTED
                    || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != GRANTED) {

                errorView.setText("Camera, microphone and storage permission not granted");
                ActivityCompat.requestPermissions(MainActivity.this, permissionRequests, 1);

            }else{
                initializeCam();
            }

        } else {
            // No camera on this device
            errorView.setText("Cannot locate a camera on this device");
        }
    }

    public boolean hasCamera(){

        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);

    }

    public void startImagePostView(Bitmap bitmap){

        postViewLayout.setVisibility(View.VISIBLE);
        captureLayout.setVisibility(View.GONE);
        imagePostViewIV.setImageBitmap(bitmap);

    }

    public void stopImagePostView(){

        captureLayout.setVisibility(View.VISIBLE);
        postViewLayout.setVisibility(View.GONE);
        mCurrentBitmapData = null;
        imageSaved = false;

    }

    //------------------------------  ALL CAMERA INITIALIZATION STUFF ENDS HERE  -----------------------------------------


    //------------------------------  ALL VIDEO CAPTURE STUFF BELOW  -----------------------------------------


    public int getMediaRecorderOrientation(){

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(manager.getCameraIdList()[mCameraId]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        int mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int finalRotation = 0;
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                finalRotation = DEFAULT_ORIENTATIONS.get(rotation);
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                finalRotation = INVERSE_ORIENTATIONS.get(rotation);
                break;
        }

        return finalRotation;

    }

    //------------------------------  ALL VIDEO CAPTURE STUFF ENDS HERE  -----------------------------------------


}
