
import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { RNGeofence } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNGeofence || {});

export const GeofenceEvent = {
    Enter: RNGeofence.ENTER_GEOFENCE,
    Exit: RNGeofence.EXIT_GEOFENCE,
}

const Geofence = {};
const GeofenceConfigSchema = {
    "title": "GeofenceConfig",
    "type": "object",
    "properties": {
        "identifier": { 
            "type": "string", 
            "description": "Unique identifier of each geofence. Needed for removing specific geofence and identified entered/exit regions" 
        },
        "latitude": { 
            "type": "number"
        },
        "longitude": { 
            "type": "number"
        },
        "radius": { 
            "type": "integer"
        },
    },
    "required": ["identifier", "latitude", "longitude", "radius"]
};
Geofence.hasPlayServices = (params = { autoResolve: true }) => {
    if(Platform.OS === 'ios') return Promise.resolve(true);
    else return RNGeofence.playServicesAvailable(params.autoResolve);
};
Geofence.locationServicesEnabled = (params = { autoResolve: true }) => {
    return RNGeofence.locationServicesEnabled();
};
Geofence.openAndroidLocationSettings = () => Platform.OS == 'android' ? RNGeofence.openAndroidLocationSettings() : false;
Geofence.initialize = () => RNGeofence.initialize();
Geofence.addGeofence = (config) => RNGeofence.addGeofence(config);  // Refer to GeofenceConfigSchema
Geofence.addGeofences = (configs) => RNGeofence.addGeofences(configs);  // Refer to GeofenceConfigSchema
Geofence.removeGeofence = (identifier) => RNGeofence.removeGeofence(identifier);
Geofence.removeAllGeofences = () => RNGeofence.removeAllGeofences();
Geofence.on = (event, callback) => {
    if (!Object.values(GeofenceEvent).includes(event)) {
        throw new Error(`Invalid geofence event subscription, use import {GeofenceEvent} from 'RNGeofence' to avoid typo`);
    };
    return EventEmitter.addListener(event, callback);
};
export default Geofence;