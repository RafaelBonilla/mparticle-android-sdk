package com.mparticle.demo;

import java.util.HashMap;
import java.util.Iterator;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mparticle.DeviceProperties;
import com.mparticle.MParticleAPI;

public class HomeActivity extends Activity {

    private static final String TAG = "mParticleDemo";

    private MParticleAPI mParticleAPI;
    private TextView diagnosticsTextView;
    private CheckBox optOutCheckBox;
    private CheckBox debugModeCheckBox;
    private SharedPreferences mPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        diagnosticsTextView = (TextView) findViewById(R.id.textDiagnostics);
        optOutCheckBox = (CheckBox) findViewById(R.id.checkBoxOptOut);
        debugModeCheckBox = (CheckBox) findViewById(R.id.checkBoxDebugMode);

        mPreferences = getSharedPreferences("mParticleDemoPrefs", MODE_PRIVATE);

        mParticleAPI = MParticleAPI.getInstance(this, "TestAppKey", "secret", 60);
        // for testing, the timeout is 1 minute
        mParticleAPI.setSessionTimeout(60*1000);
        mParticleAPI.enableLocationTracking(LocationManager.PASSIVE_PROVIDER, 15*1000, 50);

        boolean debugMode = mPreferences.getBoolean("debug_mode", true);
        mParticleAPI.setDebug(debugMode);
        debugModeCheckBox.setChecked(debugMode);
        optOutCheckBox.setChecked(mParticleAPI.getOptOut());
        collectDeviceProperties();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menuProxy:
            mParticleAPI.setConnectionProxy("192.168.1.100", 8080);
            Toast.makeText(this, "Now proxying requests to 192.168.1.100 port 8080", Toast.LENGTH_LONG).show();
            return true;
        case R.id.menuClose:
            this.finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void collectDeviceProperties() {
        StringBuffer diagnosticMessage=new StringBuffer();
        JSONObject appInfo = DeviceProperties.collectAppInfo(this.getApplicationContext());
        try {
            if (appInfo.length() > 0) {
                Iterator<?> deviceKeys = appInfo.keys();
                while( deviceKeys.hasNext() ){
                    String key = (String)deviceKeys.next();
                    diagnosticMessage.append(key + "=" + appInfo.get(key)+"\n");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error parsing app info JSON");
        }
        JSONObject deviceInfo = DeviceProperties.collectDeviceInfo(this.getApplicationContext());
        try {
            if (deviceInfo.length() > 0) {
                Iterator<?> deviceKeys = deviceInfo.keys();
                while( deviceKeys.hasNext() ){
                    String key = (String)deviceKeys.next();
                    diagnosticMessage.append(key + "=" + deviceInfo.get(key)+"\n");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error parsing device info JSON");
        }
        diagnosticsTextView.setText(diagnosticMessage.toString());
    }

    public void pressEventButton(View view) throws JSONException {
        switch (view.getId()) {
        case R.id.buttonA:
            mParticleAPI.logEvent("ButtonAPressed");
            break;
        case R.id.buttonB:
            mParticleAPI.logEvent("ButtonBPressed");
            mParticleAPI.setSessionProperty("testSessionProp1", "testValue1");
            mParticleAPI.setSessionProperty("testSessionProp2", "testValue1");
            break;
        case R.id.buttonC: {
            boolean on = ((ToggleButton) view).isChecked();
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("button_state", on ? "on":"off");
            mParticleAPI.logEvent("ButtonCPressed", eventData);
            break;
        }
        case R.id.viewA:
            mParticleAPI.logScreenView("View A");
            break;
        case R.id.viewB: {
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("key1", "value1");
            eventData.put("key2", "value2");
            mParticleAPI.logScreenView("View B", eventData);
            break;
        }
        case R.id.eventEndUpload: {
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("key1", "value1");
            eventData.put("key2", "value2");
            mParticleAPI.logEvent("TestEvent", eventData);
            mParticleAPI.endSession();
            mParticleAPI.upload();
            break;
        }
        }
    }

    public void pressDataButton(View view) {
        switch (view.getId()) {
        case R.id.buttonListSessions:
            startActivity(new Intent(this, SessionsListActivity.class));
            break;
        case R.id.buttonListMessages:
            startActivity(new Intent(this, MessagesListActivity.class));
            break;
        case R.id.buttonListUploads:
            startActivity(new Intent(this, UploadsListActivity.class));
            break;
        case R.id.buttonListCommands:
            startActivity(new Intent(this, CommandsListActivity.class));
            break;
        }
    }

    public void pressSessionButton(View view) {
        switch (view.getId()) {
        case R.id.buttonStartSession:
            mParticleAPI.start();
            break;
        case R.id.buttonStopSession:
            mParticleAPI.stop();
            break;
        case R.id.buttonNewSession:
            mParticleAPI.newSession();
            break;
        case R.id.buttonEndSession:
            mParticleAPI.endSession();
            break;
        case R.id.buttonUpload:
            mParticleAPI.upload();
            break;
        }
    }

    public void pressSetVariable(View view) {
        switch (view.getId()) {
        case R.id.buttonSetUserVar:
            TextView editUserView = (TextView) findViewById(R.id.editUserVar);
            String userVar = editUserView.getText().toString();
            mParticleAPI.setUserProperty("user_var", userVar);
            break;
        case R.id.buttonSetSessionVar:
            TextView editSessionView = (TextView) findViewById(R.id.editSessionVar);
            String sessionVar = editSessionView.getText().toString();
            mParticleAPI.setSessionProperty("session_var", sessionVar);
            break;
        }
    }

    public void pressError(View view) {
        mParticleAPI.logErrorEvent("ErrorOccurred");
    }
    public void pressCrash(View view) {
        throw new Error("Intentionally crashing demo app");
    }

    public void pressUpdateLocation(View view) {
        Random r = new Random();
        Location location = new Location("user");
        location.setLatitude( 360.0 * r.nextDouble() - 180.0 );
        location.setLongitude( 360.0 * r.nextDouble() - 180.0 );
        location.setAccuracy( 50.0f * r.nextFloat() );
        mParticleAPI.setLocation(location);
    }

    public void pressOptOut(View view) {
        boolean optOut = ((CheckBox) view).isChecked();
        mParticleAPI.setOptOut(optOut);
    }
    public void pressDebug(View view) {
        boolean debugMode = ((CheckBox) view).isChecked();
        mPreferences.edit().putBoolean("debug_mode", debugMode).commit();
        mParticleAPI.setDebug(debugMode);
    }
    public void pressPushRegistration(View view) {
        boolean pushRegistration = ((CheckBox) view).isChecked();
        if (pushRegistration) {
            mParticleAPI.setPushRegistrationId("TOKEN");
        } else {
            mParticleAPI.clearPushRegistrationId();
        }
    }

}
