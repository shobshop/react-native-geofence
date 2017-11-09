#import <React/RCTBridgeModule.h>
#import <React/RCTLog.h>
#import <React/RCTEventEmitter.h>
#import <CoreLocation/CoreLocation.h>

@interface RNGeofence : RCTEventEmitter <RCTBridgeModule,CLLocationManagerDelegate>
@property (nonatomic, strong) CLLocationManager *locationManager;
@end
  
