package com.example.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main3Activity extends AppCompatActivity  {
    public SurfaceView surfaceView;
    public SurfaceHolder surfaceHolder;
    public android.hardware.Camera camera;
    private static PowerManager.WakeLock wakeLock;
    private StorageReference mStorage;
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;




    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

//        surfaceView = findViewById(R.id.surfaceView);
//        surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.addCallback(this);
        PowerManager pm=(PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock=pm.newWakeLock(PowerManager.FULL_WAKE_LOCK|
                PowerManager.ACQUIRE_CAUSES_WAKEUP|
                PowerManager.ON_AFTER_RELEASE,"tag");
        wakeLock.acquire();


        mStorage = FirebaseStorage.getInstance().getReference();
        Toast.makeText(getApplicationContext(), "aslmdamsd !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example
        //CapturePhoto(getApplicationContext());

        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
    }
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(Main3Activity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(Main3Activity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Main3Activity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Main3Activity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(Main3Activity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }
}

//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        Log.d("kkkk","Preparing to take photo");
//        Toast.makeText(getApplicationContext(),"Preparing to take photo",Toast.LENGTH_SHORT).show();
//        camera = null;
//
//        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//
//        int frontCamera = 0;
//        //int backCamera=0;
//
//        Camera.getCameraInfo(frontCamera, cameraInfo);
//
//        try {
//            camera = Camera.open(frontCamera);
//        } catch (RuntimeException e) {
//            Log.d("kkkk","Camera not available: " + 1);
//            Toast.makeText(getApplicationContext(),"Camera not available",Toast.LENGTH_SHORT).show();
//            camera = null;
//            //e.printStackTrace();
//        }
//
//        surfaceView = findViewById(R.id.surfaceView);
//        surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.addCallback(this);
//
//    }
//
//    private void CapturePhoto() {
//
//
//        try {
//            if (null == camera) {
//                Log.d("kkkk","Could not get camera instance");
//                Toast.makeText(getApplicationContext(),"Could not get camera instance",Toast.LENGTH_SHORT).show();
//            } else {
//                Log.d("kkkk","Got the camera, creating the dummy surface texture");
//                Toast.makeText(getApplicationContext(),"Got the camera, creating the dummy surface texture",Toast.LENGTH_SHORT).show();
//                try {
//                    Camera.Parameters parameters = camera.getParameters();
//                    List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
//                    Camera.Size size = sizes.get(0);
//                    for(int i=0;i<sizes.size();i++)
//                    {
//                        if(sizes.get(i).width >= size.width)
//                            size = sizes.get(i);
//                        Log.i("SIZE: ",size.height + size.width + "");
//                    }
//                    //Toast.makeText(getApplicationContext(),size.height + " " + size.width ,Toast.LENGTH_LONG).show();
//                    //parameters.setPreviewSize(size.width, size.height);
//                    camera.setParameters(parameters);
//                    camera.setPreviewDisplay(surfaceHolder);
//                    camera.startPreview();
//                } catch (Exception e) {
//                    Log.d("kkkk","Could not set the surface preview texture");
//                    Toast.makeText(getApplicationContext(),"Could not set the surface preview texture",Toast.LENGTH_SHORT).show();
//                    e.printStackTrace();
//                }
//                camera.takePicture(null, null, new Camera.PictureCallback() {
//
//                    @Override
//                    public void onPictureTaken(byte[] data, Camera camera) {
//                        File pictureFileDir=new File("/sdcard/CaptureByService");
//
//                        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
//                            pictureFileDir.mkdirs();
//                        }
//                        Calendar c1 = Calendar.getInstance();
//                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy mm dd hh mm ss");
//                        //String date = dateFormat.format(new Date());
//                        String date = dateFormat.format(c1.getTime());
//                        String photoFile = date + ".jpg";
//                        String filename = pictureFileDir.getPath() + File.separator + photoFile;
//                        File mainPicture = new File(filename);
//
//                        try {
//                            Context context1 = getApplicationContext();
//                            FileOutputStream fos = new FileOutputStream(mainPicture);
//                            fos.write(data);
//
//
//                            Calendar c = Calendar.getInstance();
//                            System.out.println("Current time => "+c.getTime());
//
//                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                            String formattedDate = df.format(c.getTime());
//                            StorageReference riversRef = mStorage.child("images/S4-2.jpg/"+ formattedDate);
//////                            riversRef.getName().equals(riversRef.getName());
////
////                            riversRef.putBytes(data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
////                                @Override
////                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
////                                    progressDialog.dismiss();
////                                    Toast.makeText(context,"File Uploaded",Toast.LENGTH_SHORT).show();
////
////
////                                }
////                            }).
////                                    addOnFailureListener(new OnFailureListener() {
////                                        @Override
////                                        public void onFailure(@NonNull Exception e) {
////                                            Toast.makeText(context,e.getMessage(),Toast.LENGTH_LONG).show();
////
////                                        }
////                                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
////                                @Override
////                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
////                                    double progress = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
////                                    progressDialog.setMessage(((int)progress)+"% Uploaded.. ");
////                                }
////                            });
//                            final ProgressDialog progressDialog = new ProgressDialog(Main3Activity.this);
//                            progressDialog.setTitle("Uploading....");
//                            riversRef.getName().equals(riversRef.getName());    // true
//                            riversRef.getPath().equals(riversRef.getPath());
//                            UploadTask uploadTask = riversRef.putBytes(data);
//                            uploadTask.addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception exception) {
//                                    // Handle unsuccessful uploads
//                                    Toast.makeText(Main3Activity.this,exception.getMessage(),Toast.LENGTH_LONG).show();
//                                }
//                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                                @Override
//                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
//                                    // ...
//                                    progressDialog.dismiss();
////                                    mStorage.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
////                                        @Override
////                                        public void onSuccess(Uri uri) {
////                                            String url = uri.toString();
////                                            DatabaseReference newPost = mDatabase.push();
////
////                                            newPost.child("image").setValue(url));
////
////                                        }
////                                    });
//
//                                    Toast.makeText(Main3Activity.this,"File Uploaded",Toast.LENGTH_SHORT).show();
////
//                                }
//                            });
//
//
//                            fos.close();
//                            Log.d("kkkk","image saved");
//                            Toast.makeText(context1,"image saved",Toast.LENGTH_SHORT).show();
//
//                        } catch (Exception error) {
//                            Context context1 = getApplicationContext();
//
//                            Log.d("kkkk","Image could not be saved");
//                            Toast.makeText(context1,"image could not be saved" ,Toast.LENGTH_SHORT).show();
//
//                        }
//                        camera.release();
//                    }
//
//                });
//            }
//        } catch (Exception e) {
//            camera.release();
//        }
////        Intent intent = new Intent(Intent.ACTION_MAIN);
////        intent.addCategory(Intent.CATEGORY_HOME);
////        startActivity(intent);
//    }
//
//    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
//        // TODO Auto-generated method stub
//        CapturePhoto();
//    }
//
//    public void surfaceCreated(SurfaceHolder holder) {
//        // TODO Auto-generated method stub
//    }
//
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        // TODO Auto-generated method stub
//    }}
