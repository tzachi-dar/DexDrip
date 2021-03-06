package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Date;


public class StopSensor extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Stop Sensor";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    public Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Sensor.isActive() == false) {
            Intent intent = new Intent(this, StartNewSensor.class);
            startActivity(intent);
            finish();
        } else {
            setContentView(R.layout.activity_stop_sensor);
            button = (Button)findViewById(R.id.stop_sensor);
            addListenerOnButton();
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stop_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.stop_sensor);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Sensor sensor = Sensor.currentSensor();
                sensor.stopped_at = new Date().getTime();
                Log.w("NEW SENSOR", "Sensor stopped at " + sensor.stopped_at);
                sensor.save();

                Toast.makeText(getApplicationContext(), "Sensor stopped", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
    }
}
