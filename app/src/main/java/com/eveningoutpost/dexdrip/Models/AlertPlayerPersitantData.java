
package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;

/**
 * Created by tzachid on 30/1/15.
 * 
 * Class is used to store data for AlertPlayer.
 * Since there is only one AlertPlayer there will only be one record in this table
 * 
 */
@Table(name = "AlertPlayerPersitantData", id = BaseColumns._ID)
public class AlertPlayerPersitantData extends Model {
    @Column(name = "fileName")
    public String fileName;

    @Column(name = "playTime")
    public int playTime;
    
    @Column(name = "alertUuid")
    public String alertUuid;

    private final static String TAG = AlertPlayer.class.getSimpleName();
    
    public static AlertPlayerPersitantData getOnly() {
        AlertPlayerPersitantData appd = new Select()
                .from(AlertPlayerPersitantData.class)
                .orderBy("_ID asc")
                .executeSingle();
        
        if (appd != null) {
            Log.e(TAG, "getOnly appd.fileName = " + appd.fileName + "appd.playTime = " + appd.playTime);
        } else {
            Log.e(TAG, "getOnly returning null");
        }
        
        return appd;
    }
    
    public static void saveData(String fileName, int playTime) {
        Log.e(TAG, "saveData called");
        AlertPlayerPersitantData appd = getOnly();
        if (appd == null) {
            appd = new AlertPlayerPersitantData();
        }
        appd.fileName = fileName;
        appd.playTime = playTime;
        appd.save();
    }
    
    public static void ClearData() {
        Log.e(TAG, "ClearData called");
        AlertPlayerPersitantData appd = getOnly();
        if (appd != null) {
            appd.delete();
        }
    }
    

}