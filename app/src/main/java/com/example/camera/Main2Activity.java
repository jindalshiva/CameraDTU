package com.example.camera;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Toast;

public class Main2Activity extends AppCompatActivity {
    public SurfaceView surfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //surfaceView = findViewById(R.id.surfaceView);
        Intent intentAlarm = new Intent(this, MainActivity.class);

        // create the object
        long repeatInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES/(15*60);
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;
        AlarmManager am =( AlarmManager)getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intentAlarm, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, repeatInterval, pi); // Millisec
//        alarmManager.setInexactRepeating
//                (AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        triggerTime, repeatInterval, notifyPendingIntent);
        Toast.makeText(this, "Alarm Scheduled for Tommrrow", Toast.LENGTH_LONG).show();
//        Intent intentAlarm = new Intent(this, MainActivity.class);

    }
}
