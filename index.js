
import { NativeModules, NativeEventEmitter } from 'react-native';

const { RNGeofence } = NativeModules;
const EventEmitter = new NativeEventEmitter(RNGeofence || {});

export const GeofenceEvent = {
    Enter: RNGeofence.ENTER_GEOFENCE,
    Exit: RNGeofence.EXIT_GEOFENCE,
}

const Geofence = {};
Geofence.initialize = () => RNGeofence.initialize();
Geofence.addGeofence = (config) => RNGeofence.addGeofence(config);
Geofence.addGeofences = (configs) => RNGeofence.addGeofences(configs);
Geofence.removeGeofence = (identifier) => RNGeofence.removeGeofence(identifier);
Geofence.removeAllGeofences = () => RNGeofence.removeAllGeofences();
Geofence.on = (event, callback) => {
    if (!Object.values(GeofenceEvent).includes(event)) {
        throw new Error(`Invalid geofence event subscription, use import {GeofenceEvent} from 'RNGeofence' to avoid typo`);
    };
    return EventEmitter.addListener(event, callback);
};
export default Geofence;