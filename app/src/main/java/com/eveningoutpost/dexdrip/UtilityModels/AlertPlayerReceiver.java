package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlertPlayerReceiver extends BroadcastReceiver {

    private final static String TAG = AlertPlayer.class.getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.e(TAG, "New onRecieve called Threadid " + Thread.currentThread().getId());

        AlertPlayer player = AlertPlayer.getPlayer();
        player.StartAlertFromSnooz(context);

        // assumes WordService is a registered service
        //Intent intent = new Intent(context, WordService.class);
        //context.startService(intent);
    }
}