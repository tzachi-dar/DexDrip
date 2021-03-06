package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.WixelReader;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.ViewportChangeListener;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;


public class Home extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private final static String TAG = Home.class.getSimpleName();
    private String menu_name = "DexDrip";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    private LineChartView chart;
    private PreviewLineChartView previewChart;
    private WixelReader mWixelReader;
    Viewport tempViewport = new Viewport();
    Viewport holdViewport = new Viewport();
    public float left;
    public float right;
    public float top;
    public float bottom;
    public boolean updateStuff;
    public boolean updatingPreviewViewport = false;
    public boolean updatingChartViewport = false;

    public BgGraphBuilder bgGraphBuilder;
    BroadcastReceiver _broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_bg_notification, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_data_sync, false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            startService(new Intent(this, DexCollectionService.class));
        }
        setContentView(R.layout.activity_home);

        // Start the socket thread
        try
        {
            mWixelReader = new WixelReader(this);
            mWixelReader.start();
        }catch(IOException e)
        {
           Log.e(TAG, "cought IOException, could not start the wixel thread");
        }

    }
    @Override
    public void onDestroy()
	{
        super.onDestroy();
        // stop the socket thread
        mWixelReader.Stop();
        try {
            mWixelReader.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "cought InterruptedException, could not wait for the wixel thread to exit");
        }

    }

    @Override
    protected void onResume(){
        super.onResume();
        _broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    updateCurrentBgInfo();
                }
            }
        };
        registerReceiver(_broadcastReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
        holdViewport.set(0, 0, 0, 0);
        setupCharts();
        updateCurrentBgInfo();
    }

    public void setupCharts() {
        bgGraphBuilder = new BgGraphBuilder(this);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);

        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);
        previewChart.setZoomType(ZoomType.HORIZONTAL);

        chart.setLineChartData(bgGraphBuilder.lineData());
        previewChart.setLineChartData(bgGraphBuilder.previewLineData());
        updateStuff = true;

        previewChart.setViewportCalculationEnabled(true);
        chart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new ViewportListener());
        chart.setViewportChangeListener(new ChartViewPortListener());
        setViewport();

    }

    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport, false);
                updatingChartViewport = false;
            }
        }
    }

    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport, false);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff == true) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right  >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart), false);
        } else {
            previewChart.setCurrentViewport(holdViewport, false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (_broadcastReceiver != null)
            unregisterReceiver(_broadcastReceiver);
    }

    public void updateCurrentBgInfo() {
        final TextView currentBgValueText = (TextView) findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        notificationText.setText("");
        if(ActiveBluetoothDevice.first() != null || mWixelReader.IsConfigured()) {
            if (Sensor.isActive() && (Sensor.currentSensor().started_at + (60000 * 60 * 2)) < new Date().getTime()) {
                if (BgReading.latest(2).size() > 1) {
                    List<Calibration> calibrations = Calibration.latest(2);
                    if (calibrations.size() > 1) {
                        if (calibrations.get(0).slope <= 0.5 || calibrations.get(0).slope >= 1.4) {
                            notificationText.setText("Possible bad calibration slope, recommend double calibration");
                        }
                        displayCurrentInfo();
                    } else {
                        notificationText.setText("Please enter two calibrations to get started!");
                    }
                } else {
                    notificationText.setText("Please wait, need 2 readings from transmitter first.");
                }
            } else if (Sensor.isActive() && ((Sensor.currentSensor().started_at + (60000 * 60 * 2))) >= new Date().getTime()) {
                double waitTime = ((Sensor.currentSensor().started_at + (60000 * 60 * 2)) - (new Date().getTime())) / (60000) ;
                notificationText.setText("Please wait while sensor warms up! ("+ String.format("%.2f", waitTime)+" minutes)");
            } else {
                notificationText.setText("Now start your sensor");
            }
        } else {
            notificationText.setText("First pair with your BT device, or configure your wifi wixel reader!");
        }
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    public void displayCurrentInfo() {


        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);

        final TextView currentBgValueText = (TextView)findViewById(R.id.currentBgValueRealTime);
        final TextView notificationText = (TextView)findViewById(R.id.notices);
        if ((currentBgValueText.getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) > 0) {
            currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        BgReading lastBgreading = BgReading.lastNoSenssor();

        if (lastBgreading != null) {
            double estimate =0;
            if ((new Date().getTime()) - (60000 * 11) - lastBgreading.timestamp > 0) {
                notificationText.setText("Signal Missed");
                estimate = BgReading.estimated_bg(lastBgreading.timestamp + (6000 * 7));
                currentBgValueText.setText(df.format(estimate));
                currentBgValueText.setPaintFlags(currentBgValueText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                if (lastBgreading != null) {
                    estimate = BgReading.activePrediction();
                    currentBgValueText.setText(df.format(estimate) + " " + BgReading.slopeArrow());
                }
            }
            if(estimate <= bgGraphBuilder.lowMark) {
                currentBgValueText.setTextColor(Color.parseColor("#C30909"));
            } else if(estimate >= bgGraphBuilder.highMark) {
                currentBgValueText.setTextColor(Color.parseColor("#FFBB33"));
            } else {
                currentBgValueText.setTextColor(Color.WHITE);
            }
        }
    setupCharts();
    }
}
