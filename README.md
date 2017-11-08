
# react-native-geofence (Under construction)

## Getting started

`$ npm install react-native-geofence --save`

### Mostly automatic installation

`$ react-native link react-native-geofence`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-geofence` and add `RNGeofence.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNGeofence.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

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

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNGeofence.sln` in `node_modules/react-native-geofence/windows/RNGeofence.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Geofence.RNGeofence;` to the usings at the top of the file
  - Add `new RNGeofencePackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNGeofence from 'react-native-geofence';

// TODO: What to do with the module?
RNGeofence;
```
  