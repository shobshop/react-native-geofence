
package com.reactlibrary;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import android.util.Log;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient;
import android.app.PendingIntent;
import android.content.Intent;
import android.app.Activity;
import com.google.android.gms.common.ConnectionResult;
import android.os.Bundle;
import com.facebook.react.bridge.UiThreadUtil;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.GoogleApiAvailability;

public class RNGeofenceModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public static final String ENTER_GEOFENCE = "RNGeofence:EnterGeofence";
  public static final String EXIT_GEOFENCE = "RNGeofence:ExitGeofence";
  public static final String REACT_CLASS = "RNGeofenceModule";

  private GoogleApiClient mGoogleApiClient;
  private PendingIntent mGeofencePendingIntent;

  public RNGeofenceModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNGeofence";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("ENTER_GEOFENCE", ENTER_GEOFENCE);
    constants.put("EXIT_GEOFENCE", EXIT_GEOFENCE);
    return constants;
  }

  @ReactMethod
  public void initialize(final Promise promise) {
    // Create an instance of GoogleAPIClient.
    if (mGoogleApiClient == null) {
      final Activity activity = getCurrentActivity();

      if (activity == null) {
        promise.reject("NO_ACTIVITY", "NO_ACTIVITY");
        return;
      }

      activity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          GoogleApiClient.ConnectionCallbacks mConnectionListener = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
              Log.i(REACT_CLASS, "GoogleApiClient connection completed");
              promise.resolve(true);
            }

            @Override
            public void onConnectionSuspended(int i) {
              Log.i(REACT_CLASS, "GoogleApiClient connection suspended");
              promise.reject("GOOGLE_API_CONNECT_ERROR", "Connection suspended");
            }
          };
          GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
              promise.reject("GOOGLE_API_CONNECT_ERROR", "GoogleApiClient connection failed");
              Log.i(REACT_CLASS, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
            }
          };
          mGoogleApiClient = new GoogleApiClient.Builder(activity.getBaseContext())
                  .addApi(LocationServices.API)
                  .build();
          mGoogleApiClient.registerConnectionCallbacks(mConnectionListener);
          mGoogleApiClient.registerConnectionFailedListener(mConnectionFailedListener);
          mGoogleApiClient.connect();
        }
      });
    }
  }

  @ReactMethod
  public void playServicesAvailable(boolean autoresolve, Promise promise) {
    final Activity activity = getCurrentActivity();

    if (activity == null) {
      promise.reject("NO_ACTIVITY", "no activity");
      return;
    }

    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
    int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);

    if(status != ConnectionResult.SUCCESS) {
      promise.reject("" + status, "Play services not available");
      if(autoresolve && googleApiAvailability.isUserResolvableError(status)) {
        googleApiAvailability.getErrorDialog(activity, status, 2404).show();
      }
    }
    else {
      promise.resolve(true);
    }
  }

  @ReactMethod
  public void addGeofence(final ReadableMap config, final Promise promise) {
    Log.i(REACT_CLASS, "Add geofence: " + config);

    final Activity activity = getCurrentActivity();

    if (activity == null) {
      promise.reject("NO_ACTIVITY", "NO_ACTIVITY");
      return;
    }

    activity.runOnUiThread(new Runnable() {
      ResultCallback mGeofenceCreationListener = new ResultCallback() {
        @Override
        public void onResult(@NonNull Result result) {
          Log.i(REACT_CLASS, "Add geofence result = " + result);
          if(result.getStatus().isSuccess()) promise.resolve(true);
          else promise.reject("ADD_GEOFENCE_FAILED", "Add geofence failed: " + result.getStatus().getStatusCode());
        }
      };

      @Override
      public void run() {
        Geofence geofence = createGeofence(config.getString("identifier"),
                config.getDouble("latitude"),
                config.getDouble("longitude"),
                (float)config.getDouble("radius")) ;
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(geofence),
                getGeofencePendingIntent()
        ).setResultCallback(mGeofenceCreationListener);
      }
    });
  }

  @ReactMethod
  public void addGeofences(final ReadableArray configs, final Promise promise) {
    Log.i(REACT_CLASS, "Add geofences: " + configs);
    
    final Activity activity = getCurrentActivity();

    if (activity == null) {
      promise.reject("NO_ACTIVITY", "NO_ACTIVITY");
      return;
    }

    activity.runOnUiThread(new Runnable() {
      ResultCallback mGeofenceCreationListener = new ResultCallback() {
        @Override
        public void onResult(@NonNull Result result) {
          Log.i(REACT_CLASS, "Add geofence result = " + result);
          if(result.getStatus().isSuccess()) promise.resolve(true);
          else promise.reject("ADD_GEOFENCE_FAILED", "Add geofence failed: " + result.getStatus().getStatusCode());
        }
      };

      @Override
      public void run() {
        ArrayList<Geofence> geofences = new ArrayList<Geofence>();
        for(int i=0; i<configs.size(); i++) {
          ReadableMap config = configs.getMap(i);
          Geofence geofence = createGeofence(config.getString("identifier"),
                  config.getDouble("latitude"),
                  config.getDouble("longitude"),
                  (float)config.getDouble("radius")) ;
          geofences.add(geofence);
        }
        LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                getGeofencingRequest(geofences),
                getGeofencePendingIntent()
        ).setResultCallback(mGeofenceCreationListener);
      }
    });
  }

  @ReactMethod
  public void removeGeofence(String identifier) {
    Log.i(REACT_CLASS, "Remove geofence: " + identifier);
    ArrayList<String> idList = new ArrayList<String>();
    idList.add(identifier);
    LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, idList);
  }

  @ReactMethod
  public void removeAllGeofences() {
    Log.i(REACT_CLASS, "Remove all geofences");
    LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent());
  }


  private PendingIntent getGeofencePendingIntent() {
    // Reuse the PendingIntent if we already have it.
    if (mGeofencePendingIntent != null) {
      return mGeofencePendingIntent;
    }
    Intent intent = new Intent(reactContext, GeofenceTransitionsIntentService.class);
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
    // calling addGeofences() and removeGeofences().
    Log.i(REACT_CLASS, "Geofence intent = " + intent);
    mGeofencePendingIntent = PendingIntent.getService(reactContext, 0, intent, PendingIntent.
            FLAG_UPDATE_CURRENT);
    return mGeofencePendingIntent;
  }

  private GeofencingRequest getGeofencingRequest(Geofence geofence) {
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

    // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
    // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
    // is already inside that geofence.
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
    builder.addGeofence(geofence);

    // Return a GeofencingRequest.
    return builder.build();
  }

  private GeofencingRequest getGeofencingRequest(ArrayList<Geofence> geofenceList) {
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

    // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
    // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
    // is already inside that geofence.
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
    builder.addGeofences(geofenceList);

    // Return a GeofencingRequest.
    return builder.build();
  }

  private Geofence createGeofence(String identifier, double latitude, double longitude, float radius) {
    return new Geofence.Builder()
      // Set the request ID of the geofence. This is a string to identify this geofence.
      .setRequestId(identifier)
      .setCircularRegion(latitude, longitude, radius)
      .setExpirationDuration(Geofence.NEVER_EXPIRE)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
      .build();
  }
}