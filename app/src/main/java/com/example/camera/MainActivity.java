package com.example.camera;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MainActivity extends BroadcastReceiver {

    public SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    private static final int MY_CAMERA_REQUEST_CODE = 100;

    private static PowerManager.WakeLock wakeLock;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onReceive(Context context, Intent intent)
    {
        PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        wakeLock=pm.newWakeLock(PowerManager.FULL_WAKE_LOCK|
                PowerManager.ACQUIRE_CAUSES_WAKEUP|
                PowerManager.ON_AFTER_RELEASE,"tag");
        wakeLock.acquire();

//        if (ContextCompat.checkSelfPermission(Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(new String[]{Manifest.permission.CAMERA},
//                    MY_CAMERA_REQUEST_CODE);
//        }
        // Put here YOUR code.
        Log.d("kkkk","Reached Broadcast ");

        Intent intent1 = new Intent(context, Main3Activity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent1);
//        surfaceView = m.surfaceView;
//        surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //Toast.makeText(context, "Alarm !!!!!!!!!!", Toast.LENGTH_LONG).show(); // For example
        //CapturePhoto(context, surfaceHolder);
        wakeLock.release();
    }

    public void setAlarm(Context context)
    {
        AlarmManager am =( AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        long repeatInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES/3;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;
        Intent i = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, repeatInterval, pi); // Millisec * Second * Minute
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }



    private void CapturePhoto(final Context context, SurfaceHolder surfaceHolder) {

        Log.d("kkkk","Preparing to take photo");
        Toast.makeText(context,"Preparing to take photo",Toast.LENGTH_SHORT).show();
        Camera camera = null;

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        int frontCamera = 0;
        //int backCamera=0;

        Camera.getCameraInfo(frontCamera, cameraInfo);

        try {
            camera = Camera.open(frontCamera);
        } catch (RuntimeException e) {
            Log.d("kkkk","Camera not available: " + 1);
            Toast.makeText(context,"Camera not available",Toast.LENGTH_SHORT).show();
            camera = null;
            //e.printStackTrace();
        }
        try {
            if (null == camera) {
                Log.d("kkkk","Could not get camera instance");
                Toast.makeText(context,"Could not get camera instance",Toast.LENGTH_SHORT).show();
            } else {
                Log.d("kkkk","Got the camera, creating the dummy surface texture");
                Toast.makeText(context,"Got the camera, creating the dummy surface texture",Toast.LENGTH_SHORT).show();
                try {
                    Camera.Parameters parameters = camera.getParameters();
                    List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
                    Camera.Size size = sizes.get(0);
                    for(int i=0;i<sizes.size();i++)
                    {
                        if(sizes.get(i).width > size.width)
                            size = sizes.get(i);
                    }
                    parameters.setPreviewSize(size.width/5, size.height/5);
                    parameters.setJpegQuality(100);
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//                    camera.setParameters(parameters);

                    camera.setPreviewTexture(new SurfaceTexture(0));
                    camera.startPreview();
                } catch (Exception e) {
                    Log.d("kkkk","Could not set the surface preview texture");
                    Toast.makeText(context,"Could not set the surface preview texture",Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
                camera.takePicture(null, null, new Camera.PictureCallback() {

                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        File pictureFileDir=new File("/sdcard/CaptureByService");

                        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                            pictureFileDir.mkdirs();
                        }
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy mm dd hh mm ss");
                        String date = dateFormat.format(new Date());
                        String photoFile = date + ".jpg";
                        String filename = pictureFileDir.getPath() + File.separator + photoFile;
                        File mainPicture = new File(filename);

                        try {
                            Context context1 = context;
                            FileOutputStream fos = new FileOutputStream(mainPicture);
                            fos.write(data);
                            fos.close();
                            Log.d("kkkk","image saved");
                            Toast.makeText(context1,"image saved",Toast.LENGTH_SHORT).show();

                        } catch (Exception error) {
                            Context context1 = context;

                            Log.d("kkkk","Image could not be saved");
                            Toast.makeText(context1,"image could not be saved" ,Toast.LENGTH_SHORT).show();

                        }
                        camera.release();
                    }
                });
            }
        } catch (Exception e) {
            camera.release();
        }
    }







//    public void takePictureNoPreview(Context context){
//        // open back facing camera by default
//        Camera myCamera=Camera.open();
//
//        if(myCamera!=null){
//            try{
//                //set camera parameters if you want to
//                //...
//
//                // here, the unused surface view and holder
//                SurfaceView dummy=new SurfaceView(context)
//                myCamera.setPreviewDisplay(dummy.getHolder());
//                myCamera.startPreview();
//
//                myCamera.takePicture(null, null, getJpegCallback()):
//
//            }finally{
//                myCamera.close();
//            }
//
//        }else{
//            //booo, failed!
//        }
//
//
//
//    }
//    private Camera.PictureCallback getJpegCallback() {
//        Camera.PictureCallback jpeg = new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                FileOutputStream fos;
//                try {
//                    fos = new FileOutputStream("test.jpeg");
//                    fos.write(data);
//                    fos.close();
//                } catch (IOException e) {
//                    //do something about it
//                }
//            }
//        };
//    }


}
