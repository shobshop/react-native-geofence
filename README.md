
# react-native-geofence

## Getting started

1. `$ npm install https://github.com/shobshop/react-native-geofence --save`
2. Do one of these:
- `$ react-native link react-native-geofence`
- or follow **Manual installation** section.
3. Follow **Shared step** section.

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-geofence` and add `RNGeofence.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNGeofence.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`



#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNGeofencePackage;` to the imports at the top of the file
  - Add `new RNGeofencePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-geofence'
  	project(':react-native-geofence').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-geofence/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-geofence')
  	```

### Shared step

#### iOS
Add the following keys to project's `info.plist`:
```
		...
		<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
		<string>This makes geofence work even app is not on foreground on iOS11+</string>
		<key>NSLocationAlwaysUsageDescription</key>
		<string>This makes geofence work even app is not on foreground</string>
		<key>NSLocationWhenInUseUsageDescription</key>
		<string>This makes geofence work only when app is on foreground</string>
	</dict>
</plist>
```

#### Android
Add the following line inside <Application> element in app's `AndroidManifest.xml`:
```
		...
		<service android:name="com.reactlibrary.GeofenceTransitionsIntentService"/>
	</application>
</manifest>
```

## Usage
```javascript
import RNGeofence, { GeofenceEvent } from 'react-native-geofence';
...
	// Check whether Google Play Services is installed (always resolve to true on iOS). If not, the flag 'autoResolve' will decide whether to show instruction to update Google Play Services.
	RNGeofence.hasPlayServices({ autoResolve: true }).then(() => RNGeofence.initialize()).then(() => {
	  console.log('Geofence initialize completed');
	  
	  // Listen to geofence enter events
      RNGeofence.on(GeofenceEvent.Enter, ({identifier}) => {
        console.log('Enter geofence: ' + identifier);
	  });
	  // Listen to geofence exit events
      RNGeofence.on(GeofenceEvent.Exit, ({identifier}) => {
        console.log('Exit geofence: ' + identifier);
	  });

	  // Add and start monitoring a geofence
	  RNGeofence.addGeofence({
		identifier: 'Central World',
		latitude: 13.746596,
		longitude: 100.5392832,
		radius: 250
	  });

	  // Add and start monitoring multiple geofences. The same identifiers will overwrite existing ones.
      RNGeofence.addGeofences([{
          identifier: 'Siam Center',
          latitude: 13.746226,
          longitude: 100.532803,
          radius: 150
        },{
          identifier: 'Siam Paragon',
          latitude: 13.746919,
          longitude: 100.535047,
          radius: 200
        }
      ]);
	  // Remove specific geofence
	  RNGeofence.removeGeofence('Siam Paragon');
	  
	  // Remove all geofences
      RNGeofence.removeAllGeofences();
    })
    .catch((err) => {
      console.log("Geofence initialization failed: ", err.code, err.message);
    })
```
  