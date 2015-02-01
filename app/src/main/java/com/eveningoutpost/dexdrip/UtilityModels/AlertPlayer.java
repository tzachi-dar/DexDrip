package com.eveningoutpost.dexdrip.UtilityModels;

//import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Models.AlertPlayerPersitantData;



/*

1) Create a class AlertsPlayer with the following functionality:
a) Play file. (file name, repeat every n seconds)
b) stop playing.
c) snooze for x minutes.

Notes:
Only ui thread is allowed to call it.
Only one file can be played at a time, the last one wins.
If file does not exist, play a default file from the resource...
The callers don't have to know anything about it's state. that is stop before start is allowed and so on...
the AlertsPlayer will manage it's timers and continue even when the program is stopped, until stop was called.

Should have state written to disk????



//??????????????? verify thread id ??????????

*/




public class AlertPlayer {

    static AlertPlayer singletone;
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    private MediaPlayer mediaPlayer;
    // not playing, playing, finished
    int state;
    
    
    public static AlertPlayer getPlayer() {
        if(singletone == null) {
            Log.e(TAG,"Creating a new PlayFile");
            singletone = new AlertPlayer();
        } else {
            Log.e("tag","Using existing PlayFile");
        }
        return singletone;
    }
    
    public synchronized  void StartAlertFromSnooz(Context ctx) {
        AlertPlayerPersitantData appd = AlertPlayerPersitantData.getOnly();
        if(appd != null) {
            startAlert(ctx, appd.playTime, appd.fileName );
        } else {
            Log.e(TAG, "Start music ignored since there is no stored context");
        }
    }

    public synchronized  void startAlert(Context ctx, int repeatTime, String fileName, )  {
      Log.e(TAG, "start called, Threadid " + Thread.currentThread().getId());
      stopMusic(ctx, false);
      AlertPlayerPersitantData.saveData(fileName, repeatTime);
      
      mediaPlayer = MediaPlayer.create(ctx, Uri.parse(fileName), null);
      if(mediaPlayer == null) {
          Log.w(TAG, "Creating mediaplayer with file " + fileName + " failed. using default alarm");
          mediaPlayer = MediaPlayer.create(ctx, R.raw.default_alert);
      }
      if(mediaPlayer != null) {
          mediaPlayer.start();
      }
      
      ArmTimer(ctx, repeatTime);
      
    }

    public synchronized void stopMusic(Context ctx, boolean clearData) {
        Log.e(TAG, "stop called ThreadID" + Thread.currentThread().getId());
        ClearTimer(ctx);
        if (clearData) {
            AlertPlayerPersitantData.ClearData();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    public synchronized  void Snooze(Context ctx, int repeatTime) {
        Log.e(TAG, "Snooze called");
        stopMusic(ctx, false);
        ArmTimer(ctx, repeatTime);
    }
    
    private synchronized void  ArmTimer(Context ctx, int time) {
        Log.e(TAG, "ArmTimer called");
        Intent intent = new Intent();
        intent.setAction("com.eveningoutpost.dexdrip.alertmanager.UtilityModels");

        AlarmManager alarmMgr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(ctx, 0, intent, 0);

        alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() +
                        time , alarmIntent);
    }
    
    private synchronized void ClearTimer(Context ctx) {
        Intent intent = new Intent();
        intent.setAction("com.eveningoutpost.dexdrip.alertmanager.UtilityModels");
        PendingIntent.getBroadcast(ctx, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT).cancel();
    }
}




