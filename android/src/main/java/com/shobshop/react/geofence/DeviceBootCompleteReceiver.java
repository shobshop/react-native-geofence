package com.shobshop.react.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;

/**
 * Created by Rasita on 12/19/2017 AD.
 */

public class DeviceBootCompleteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.i("DeviceBootComplete", "DeviceBootCompleteReceiver onReceive action = " + intent.getAction());
        if ((intent.getAction().equals("android.location.MODE_CHANGED") && isLocationModeAvailable(context))
                || (intent.getAction().equals("android.location.PROVIDERS_CHANGED") && isLocationServciesAvailable(context))
                || intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Runnable myRunnable = new Runnable() {
                @Override
                public void run() {
                    ReactApplication reactApplication = ((ReactApplication) context.getApplicationContext());
                    ReactNativeHost mReactNativeHost = reactApplication.getReactNativeHost();
                    final ReactInstanceManager reactInstanceManager = mReactNativeHost.getReactInstanceManager();
                    ReactContext reactContext = reactInstanceManager.getCurrentReactContext();

                    if (reactContext == null) {
                        reactInstanceManager
                                .addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                                    @Override
                                    public void onReactContextInitialized(ReactContext reactContext) {
                                        // Send onDeviceBootCompleted event to RN
                                        RNGeofencePackage.emitMessageToRN(reactContext, RNGeofenceModule.DEVICE_BOOT_COMPLETED, null);
                                        reactInstanceManager.removeReactInstanceEventListener(this);
                                    }
                                });
                        if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
                            reactInstanceManager.createReactContextInBackground();
                        }
                    } else {
                        // Send onDeviceBootCompleted event to RN
                        RNGeofencePackage.emitMessageToRN(reactContext, RNGeofenceModule.DEVICE_BOOT_COMPLETED, null);
                    }
                }
            };

            UiThreadUtil.runOnUiThread(myRunnable);
        }
    }

    // Snippets from https://stackoverflow.com/questions/29671039/geofences-not-working-when-app-is-killed/30773062#30773062
    private boolean isLocationModeAvailable(Context context) {

        if (Build.VERSION.SDK_INT >= 19 && getLocationMode(context) != Settings.Secure.LOCATION_MODE_OFF) {
            return true;
        }
        else return false;
    }

    public boolean isLocationServciesAvailable(Context context) {
        if (Build.VERSION.SDK_INT < 19) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        }
        else return false;
    }

    public int getLocationMode(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return 0;
    }

}
