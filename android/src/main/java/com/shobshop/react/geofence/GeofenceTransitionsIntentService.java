package com.shobshop.react.geofence;

import android.app.IntentService;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import android.content.Intent;
import android.util.Log;
import java.util.List;

/**
 * Created by Rasita on 11/9/2017 AD.
 */

public class GeofenceTransitionsIntentService extends IntentService {
    private static final String TAG = "GeofenceIntentService";

    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }
    private ReactNativeHost mReactNativeHost;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "GeofenceTransitionsIntentService onCreate");
        ReactApplication reactApplication = ((ReactApplication) getApplicationContext());
        mReactNativeHost = reactApplication.getReactNativeHost();
    }

    protected void onHandleIntent(Intent intent) {
        final GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "Error code = " + geofencingEvent.getErrorCode();
            Log.e(TAG, errorMessage);
            return;
        }

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
            final ReactInstanceManager reactInstanceManager = mReactNativeHost.getReactInstanceManager();
            ReactContext reactContext = reactInstanceManager.getCurrentReactContext();

            if (reactContext == null) {
                reactInstanceManager
                        .addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                            @Override
                            public void onReactContextInitialized(ReactContext reactContext) {
                                handleGeofenceEvent(reactContext, geofencingEvent);
                                reactInstanceManager.removeReactInstanceEventListener(this);
                            }
                        });
                if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
                    reactInstanceManager.createReactContextInBackground();
                }
            } else {
                handleGeofenceEvent(reactContext, geofencingEvent);
            }
            }
        };

        UiThreadUtil.runOnUiThread(myRunnable);
    }

    private void handleGeofenceEvent(ReactContext reactContext, GeofencingEvent geofencingEvent) {
        // Get the transition type.
        final int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            final List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    geofenceTransition,
                    triggeringGeofences
            );

            Log.i(TAG, "Geofence transition = " + geofenceTransitionDetails);

            String eventName = "";
            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) eventName = RNGeofenceModule.ENTER_GEOFENCE;
            if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) eventName = RNGeofenceModule.EXIT_GEOFENCE;
            for (Geofence geofence : triggeringGeofences) {
                WritableMap resultMap = Arguments.createMap();
                resultMap.putString("identifier", geofence.getRequestId());

                RNGeofencePackage.emitMessageToRN(reactContext, eventName, resultMap);
            }
        } else {
            // Log the error.
            Log.e(TAG, "geofence_transition_invalid_type");
        }

    }
    private String getGeofenceTransitionDetails(int geofenceTransition, List triggeringGeofences) {
        String eventName = "";
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) eventName = "Enter";
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) eventName = "Exit";
        return eventName + " geofence " + triggeringGeofences;
    }
}
