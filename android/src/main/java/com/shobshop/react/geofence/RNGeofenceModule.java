
package com.shobshop.react.geofence;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;

import android.text.TextUtils;
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
import com.google.android.gms.common.GoogleApiAvailability;

public class RNGeofenceModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private Context mContext;

  public static final String ENTER_GEOFENCE = "RNGeofence:EnterGeofence";
  public static final String EXIT_GEOFENCE = "RNGeofence:ExitGeofence";
  public static final String DEVICE_BOOT_COMPLETED = "RNGeofence:DeviceBootCompleted";
  public static final String REACT_CLASS = "RNGeofenceModule";

  private GoogleApiClient mGoogleApiClient;
  private PendingIntent mGeofencePendingIntent;

  public RNGeofenceModule(ReactApplicationContext reactContext, Context context) {
    super(reactContext);
    this.reactContext = reactContext;
    mContext = context;
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
    constants.put("DEVICE_BOOT_COMPLETED", DEVICE_BOOT_COMPLETED);
    return constants;
  }

  @ReactMethod
  public void initialize(final Promise promise) {
    // Create an instance of GoogleAPIClient.
    if (mGoogleApiClient == null) {

      UiThreadUtil.runOnUiThread(new Runnable() {
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
          mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                  .addApi(LocationServices.API)
                  .build();
          mGoogleApiClient.registerConnectionCallbacks(mConnectionListener);
          mGoogleApiClient.registerConnectionFailedListener(mConnectionFailedListener);
          mGoogleApiClient.connect();
        }
      });
    } else {
      promise.resolve(true);
    }
  }

  @ReactMethod
  public void playServicesAvailable(boolean autoresolve, Promise promise) {

    GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
    int status = googleApiAvailability.isGooglePlayServicesAvailable(mContext);

    if(status != ConnectionResult.SUCCESS) {
      promise.reject("" + status, "Play services not available");
      Activity activity = getCurrentActivity();
      if(autoresolve && googleApiAvailability.isUserResolvableError(status) && activity != null) {
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

    UiThreadUtil.runOnUiThread(new Runnable() {
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

    UiThreadUtil.runOnUiThread(new Runnable() {
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
  public void locationServicesEnabled(Promise promise) {
    int locationMode = 0;
    String locationProviders;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
      try {
        locationMode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE);

      } catch (Settings.SettingNotFoundException e) {
        e.printStackTrace();
        promise.resolve(false);
      }

      promise.resolve(locationMode != Settings.Secure.LOCATION_MODE_OFF);

    }else{
      locationProviders = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
      promise.resolve(!TextUtils.isEmpty(locationProviders));
    }
  }

  @ReactMethod
  public void removeAllGeofences() {
    Log.i(REACT_CLASS, "Remove all geofences");
    LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, getGeofencePendingIntent());
  }

  @ReactMethod
  public void openAndroidLocationSettings() {
    final Activity activity = getCurrentActivity();
    final String action = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
    activity.startActivity(new Intent(action));
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