
#import "RNGeofence.h"

static NSString *const ENTER_GEOFENCE = @"RNGeofence:EnterGeofence";
static NSString *const EXIT_GEOFENCE = @"RNGeofence:ExitGeofence";

@implementation RNGeofence

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

- (NSDictionary *)constantsToExport
{
    return @{
        @"ENTER_GEOFENCE": ENTER_GEOFENCE,
        @"EXIT_GEOFENCE": EXIT_GEOFENCE,
    };
}

- (NSArray<NSString *> *)supportedEvents {
    return @[ENTER_GEOFENCE, EXIT_GEOFENCE];
}

+ (BOOL)requiresMainQueueSetup {
    return YES;
}

RCT_EXPORT_METHOD(initialize:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    if(![CLLocationManager locationServicesEnabled]) {
        // handle this
        NSDictionary *userInfo = @{
                                   NSLocalizedDescriptionKey: NSLocalizedString(@"Location services not enabled", nil),
                                   NSLocalizedFailureReasonErrorKey: NSLocalizedString(@"Location services not enabled", nil),
                                   NSLocalizedRecoverySuggestionErrorKey: NSLocalizedString(@"Turn on location services in iOS Settings and try again", nil)
                                   };
        NSError *error = [NSError errorWithDomain:[[NSBundle mainBundle] bundleIdentifier] code:-100 userInfo:userInfo];
        reject(@"location_services_not_enabled", @"Location services not enabled", error);
        return;
    }
    if(![CLLocationManager isMonitoringAvailableForClass:[CLCircularRegion class]]) {
        NSDictionary *userInfo = @{
                                   NSLocalizedDescriptionKey: NSLocalizedString(@"Monitoring is not available for this device", nil),
                                   NSLocalizedFailureReasonErrorKey: NSLocalizedString(@"This device does not support region monitoring", nil),
                                   NSLocalizedRecoverySuggestionErrorKey: NSLocalizedString(@"", nil)
                                   };
        NSError *error = [NSError errorWithDomain:[[NSBundle mainBundle] bundleIdentifier] code:-101 userInfo:userInfo];
        reject(@"monitoring_not_supported", @"Monitoring is not available for this device", error);
        return;
    }
    self.locationManager = [[CLLocationManager alloc] init];
    if ([self.locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
        [self.locationManager requestAlwaysAuthorization];
    }
    RCTLogInfo(@"Initialize geofence monitoring");
//    RCTLogInfo(@"Monitored geofences = %@",[_locationManager monitoredRegions]);
    self.locationManager.delegate = self;
    resolve(@[]);
}

RCT_EXPORT_METHOD(addGeofence:(NSDictionary*)config) {
    if (self.locationManager == nil) {
        [NSException raise:@"Location Manager Not Initialized" format:@"You must initialize location manager first."];
    }
    
    CLRegion *region = [self mapDictionaryToRegion:config];
    dispatch_async(dispatch_get_main_queue(), ^{
        [_locationManager startMonitoringForRegion:region];
    });
}

RCT_EXPORT_METHOD(addGeofences:(NSArray*) configs) {
    if (self.locationManager == nil) {
        [NSException raise:@"Location Manager Not Initialized" format:@"You must initialize location manager first."];
    }
    
    dispatch_async(dispatch_get_main_queue(), ^{
        for(NSDictionary *regionDict in configs) {
            CLRegion *region = [self mapDictionaryToRegion:regionDict];
            [_locationManager startMonitoringForRegion:region];
        }
    });
}

RCT_EXPORT_METHOD(removeGeofence:(NSString*)identifier) {
    if (self.locationManager == nil) {
        [NSException raise:@"Location Manager Not Initialized" format:@"You must initialize location manager first."];
    }
    RCTLogInfo(@"Remove geofence = %@",identifier);
    for(CLRegion *region in [_locationManager monitoredRegions]) {
        if([region.identifier isEqualToString:identifier]) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [_locationManager stopMonitoringForRegion: region];
                return;
            });
        }
    }
}

RCT_EXPORT_METHOD(removeAllGeofences) {
    if (self.locationManager == nil) {
        [NSException raise:@"Location Manager Not Initialized" format:@"You must initialize location manager first."];
    }
    RCTLogInfo(@"Remove all geofences");
    dispatch_async(dispatch_get_main_queue(), ^{
        for(CLRegion *region in [_locationManager monitoredRegions]) {
            [_locationManager stopMonitoringForRegion: region];
        }
    });
}

- (CLCircularRegion*)mapDictionaryToRegion:(NSDictionary*)dictionary
{
    NSString *identifier = [dictionary valueForKey:@"identifier"];
    
    CLLocationDegrees latitude = [[dictionary valueForKey:@"latitude"] doubleValue];
    CLLocationDegrees longitude =[[dictionary valueForKey:@"longitude"] doubleValue];
    CLLocationCoordinate2D centerCoordinate = CLLocationCoordinate2DMake(latitude, longitude);
    
    CLLocationDistance regionRadius = [[dictionary valueForKey:@"radius"] doubleValue];
    return [[CLCircularRegion alloc] initWithCenter:centerCoordinate
                                             radius: regionRadius
                                         identifier: identifier];
    
}

- (void) emitMessageToRN: (NSString *)eventName
                        :(NSDictionary *)params{
    [self sendEventWithName:eventName body:params];
}

// MARK: - CLLocationManagerDelegate

- (void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region
{
    RCTLogInfo(@"Enter geofence %@", region.identifier);
    [self emitMessageToRN:ENTER_GEOFENCE :@{@"identifier": region.identifier}];
}

- (void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region
{
    RCTLogInfo(@"Exit geofence %@", region.identifier);
    [self emitMessageToRN:EXIT_GEOFENCE :@{@"identifier": region.identifier}];
}
- (void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region
{
    RCTLogInfo(@"Start monitoring geofence %@", region.identifier);
}
- (void)locationManager:(CLLocationManager *)manager monitoringDidFailForRegion:(nullable CLRegion *)region withError:(nonnull NSError *)error
{
    RCTLogInfo(@"Monitoring geofence %@ failed: %@", region.identifier, error.description);
}
@end
  
