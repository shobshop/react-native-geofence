
#import "RNGeofence.h"

static NSString *const ENTER_GEOFENCE = @"RNGeofence:EnterGeofence";
static NSString *const EXIT_GEOFENCE = @"RNGeofence:ExitGeofence";
static int const maximumMonitoringRegionCount = 20;
static NSString *const superRegionIdentifier = @"RNGeofence:SUPER";
static NSString *const regionsKey = @"RNGeofence:regions";

@implementation RNGeofence
BOOL refreshing = false;

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
    int savedRegionsCount = [self countSavedRegions];
    
    CLCircularRegion *region = [self mapDictionaryToRegion:config];
    [self saveRegion:region];
    
    if(savedRegionsCount + 1 <= maximumMonitoringRegionCount) {
        NSLog(@"Number of fences is under the limit. Start monitoring normally.");
        dispatch_async(dispatch_get_main_queue(), ^{
            [_locationManager startMonitoringForRegion:region];
        });
    } else {
        // Over the limit. Create one dummy region and another (maximumMonitoringRegionCount - 1) regions that are closest to user's current location
        NSLog(@"Number of fences is over the limit. Start getting location");
        // If over the limit, get user location and let monitorNearbyRegions() handle the monitoring
        refreshing = true;
        [_locationManager startUpdatingLocation];
    }
}

RCT_EXPORT_METHOD(addGeofences:(NSArray*) configs) {
    if (self.locationManager == nil) {
        [NSException raise:@"Location Manager Not Initialized" format:@"You must initialize location manager first."];
    }
    int savedRegionsCount = [self countSavedRegions];
    NSMutableArray<CLCircularRegion *> *regionsToMonitor = [NSMutableArray array];
    for(NSDictionary *regionDict in configs) {
        CLCircularRegion *region = [self mapDictionaryToRegion:regionDict];
        [regionsToMonitor addObject:region];
    }
    [self saveRegions:regionsToMonitor];
    if(savedRegionsCount + configs.count <= maximumMonitoringRegionCount) {
        NSLog(@"Number of fences is under the limit. Start monitoring normally.");
        dispatch_async(dispatch_get_main_queue(), ^{
            for(CLCircularRegion *region in regionsToMonitor) {
                [_locationManager startMonitoringForRegion:region];
            }
        });
    } else {
        // Over the limit. Create one dummy region and another (maximumMonitoringRegionCount - 1) regions that are closest to user's current location
        NSLog(@"Number of fences is over the limit. Start getting location");
        // If over the limit, get user location and let monitorNearbyRegions() handle the monitoring
        refreshing = true;
        [_locationManager startUpdatingLocation];
    }
}

RCT_EXPORT_METHOD(removeGeofence:(NSString*)identifier) {
    if (self.locationManager == nil) {
        [NSException raise:@"Location Manager Not Initialized" format:@"You must initialize location manager first."];
    }
    RCTLogInfo(@"Remove geofence = %@",identifier);
    for(CLRegion *region in [_locationManager monitoredRegions]) {
        if([region.identifier isEqualToString:identifier]) {
            int savedRegionsCount = [self countSavedRegions];
            dispatch_async(dispatch_get_main_queue(), ^{
                [_locationManager stopMonitoringForRegion: region];
                [self deleteSavedRegion:region.identifier];
                if(savedRegionsCount - 1 > maximumMonitoringRegionCount) {
                    // Over the limit. Create one dummy region and another (maximumMonitoringRegionCount - 1) regions that are closest to user's current location
                    NSLog(@"Number of fences is over the limit. Start getting location");
                    // If over the limit, get user location and let monitorNearbyRegions() handle the monitoring
                    refreshing = true;
                    [_locationManager startUpdatingLocation];
                }
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
            [self deleteSavedRegion:region.identifier];
        }
    });
}

RCT_EXPORT_METHOD(locationServicesEnabled:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@([CLLocationManager locationServicesEnabled]));
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
    if([region.identifier isEqualToString:superRegionIdentifier]) {
        NSLog(@"Exit super geofence. Refreshing geofences");
        refreshing = true;
        [_locationManager startUpdatingLocation];
    } else {
        [self emitMessageToRN:EXIT_GEOFENCE :@{@"identifier": region.identifier}];
    }
}
- (void)locationManager:(CLLocationManager *)manager didStartMonitoringForRegion:(CLRegion *)region
{
    RCTLogInfo(@"Start monitoring geofence %@", region.identifier);
}
- (void)locationManager:(CLLocationManager *)manager monitoringDidFailForRegion:(nullable CLRegion *)region withError:(nonnull NSError *)error
{
    RCTLogInfo(@"Monitoring geofence %@ failed: %@", region.identifier, error.description);
}
- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray<CLLocation *> *)locations
{
    NSLog(@"User locations = %@",locations);
    // If refreshing geofences, forward acquired location to geofence selection process
    if(refreshing) {
        refreshing = false;
        [_locationManager stopUpdatingLocation];
        CLLocation *userLocation = locations.lastObject;
        [self monitorNearbyRegions:userLocation];
    }
}

//
/**
 Start monitor as many geofences as permitted by iOS. Only for 20+ geofences
 Will monitor [maximumMonitoringRegionCount-1] geofences + 1 super geofence
 */
- (void)monitorNearbyRegions:(CLLocation *)userLocation
{
    // Stop monitoring all geofences
    for(CLRegion *region in [_locationManager monitoredRegions]) {
        [_locationManager stopMonitoringForRegion: region];
    }
    // Retrieve from saved
    NSArray<CLCircularRegion *> *regionsToMonitor = [self restoreAllRegions];
    // Sort geofences by distance from user location
    NSArray<CLCircularRegion *> *sortedRegions = [self sortRegions:regionsToMonitor byLocation:userLocation];
    // Truncate to [maximumMonitoringRegionCount-1] geofences
    NSArray<CLCircularRegion *> *nearbyRegions = [sortedRegions subarrayWithRange:NSMakeRange(0, maximumMonitoringRegionCount-1)];
    // Start monitoring each geofences
    for(CLCircularRegion *region in nearbyRegions) {
        CLLocation *regionCenter = [[CLLocation alloc] initWithLatitude:region.center.latitude longitude:region.center.longitude];
        NSLog(@"Monitor region %@, distance = %lf", region.identifier, [userLocation distanceFromLocation:regionCenter]);
        [_locationManager startMonitoringForRegion:region];
        [self saveRegion:region];
    }
    // Create and monitor "super geofence"
    CLCircularRegion *farthestRegion = [nearbyRegions lastObject];
    if(farthestRegion != nil) {
        CLCircularRegion *superRegion = [self createSuperRegion:farthestRegion fromLocation:userLocation];
        [_locationManager startMonitoringForRegion:superRegion];
    }
}

/**
 Sort geofences by distance ascendingly
 */
- (NSArray<CLCircularRegion *> *)sortRegions: (NSArray<CLCircularRegion *> *)regions byLocation:(CLLocation *)location
{
    return [regions sortedArrayUsingComparator:^NSComparisonResult(CLCircularRegion *region1, CLCircularRegion *region2){
        CLLocation *region1Center = [[CLLocation alloc] initWithLatitude:region1.center.latitude longitude:region1.center.longitude];
        CLLocation *region2Center = [[CLLocation alloc] initWithLatitude:region2.center.latitude longitude:region2.center.longitude];
        CLLocationDistance distance1 = [location distanceFromLocation:region1Center];
        CLLocationDistance distance2 = [location distanceFromLocation:region2Center];
        if(distance1 < distance2) {
            return (NSComparisonResult)NSOrderedAscending;
        } else if(distance1 > distance2) {
            return (NSComparisonResult)NSOrderedDescending;
        } else {
            return (NSComparisonResult)NSOrderedSame;
        }
    }];
}

/**
 Create "super geofence" (special geofence which covers the nearest [monitoringLimit - 1] geofences)
 */
- (CLCircularRegion *)createSuperRegion: (CLCircularRegion *)farthestRegion fromLocation:(CLLocation *)userLocation
{
    // radius = distance from user location to the farthest region's nearer rim
    CLLocation *farthestRegionCenter = [[CLLocation alloc] initWithLatitude:farthestRegion.center.latitude longitude:farthestRegion.center.longitude];
    CLLocationDistance superRegionRadius = [userLocation distanceFromLocation:farthestRegionCenter] - farthestRegion.radius;
    CLLocationDistance clampedRadius = MIN(superRegionRadius, _locationManager.maximumRegionMonitoringDistance);
    CLCircularRegion *superRegion = [[CLCircularRegion alloc] initWithCenter: userLocation.coordinate
                                                                      radius: clampedRadius
                                                                  identifier: superRegionIdentifier];
    // Notify on exit only
    superRegion.notifyOnEntry = false;
    superRegion.notifyOnExit = true;
    return superRegion;
}

// MARK: - Persistence

/**
 Persist a region in NSUserDefaults
 */
- (void)saveRegion: (CLCircularRegion *)region
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *regionsDict = [[defaults objectForKey:regionsKey] mutableCopy];
    if(regionsDict == nil) {
        regionsDict = [NSMutableDictionary dictionary];
    }
    NSDictionary *regionDict = @{ @"identifier": region.identifier,
                                  @"latitude": [NSNumber numberWithDouble:region.center.latitude],
                                  @"longitude": [NSNumber numberWithDouble:region.center.longitude],
                                  @"radius": [NSNumber numberWithDouble:region.radius]
                                  };
    [regionsDict setObject:regionDict forKey:region.identifier];
    [defaults setObject:regionsDict forKey:regionsKey];
    [defaults synchronize];
}

/**
 Persist regions in NSUserDefaults
 */
- (void)saveRegions: (NSArray<CLCircularRegion *> *)regions
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *regionsDict = [[defaults objectForKey:regionsKey] mutableCopy];
    if(regionsDict == nil) {
        regionsDict = [NSMutableDictionary dictionary];
    }
    for(CLCircularRegion *region in regions) {
        NSDictionary *regionDict = @{ @"identifier": region.identifier,
                                      @"latitude": [NSNumber numberWithDouble:region.center.latitude],
                                      @"longitude": [NSNumber numberWithDouble:region.center.longitude],
                                      @"radius": [NSNumber numberWithDouble:region.radius]
                                      };
        [regionsDict setObject:regionDict forKey:region.identifier];
    }
    [defaults setObject:regionsDict forKey:regionsKey];
    [defaults synchronize];
}


/**
 Get all persisted regions from NSUserDefaults
 */
- (NSArray<CLCircularRegion *> *)restoreAllRegions
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *regionsDict = [defaults objectForKey:regionsKey];
    if(regionsDict == nil) {
        return [NSArray array];
    }
    NSMutableArray<CLCircularRegion *> *regions = [NSMutableArray array];
    for(id key in regionsDict) {
        NSDictionary *regionDict = [regionsDict objectForKey:key];
        CLLocationCoordinate2D centerCoordinate = CLLocationCoordinate2DMake([[regionDict valueForKey:@"latitude"] doubleValue], [[regionDict valueForKey:@"longitude"] doubleValue]);
        CLLocationDistance regionRadius = [[regionDict valueForKey:@"radius"] doubleValue];
        CLCircularRegion *region = [[CLCircularRegion alloc] initWithCenter:centerCoordinate
                                                                     radius: regionRadius
                                                                 identifier: [regionDict valueForKey:@"identifier"]];
        [regions addObject:region];
    }
    return regions;
}

/**
 Delete persisted region from NSUserDefaults
 */
- (void)deleteSavedRegion: (NSString *)identifier
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *regionsDict = [[defaults objectForKey:regionsKey] mutableCopy];
    if(regionsDict != nil) {
        [regionsDict removeObjectForKey:identifier];
    }
    [defaults setObject:regionsDict forKey:regionsKey];
    [defaults synchronize];
}

/**
 Count number of saved regions
 */
- (int)countSavedRegions
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSMutableDictionary *regionsDict = [defaults objectForKey:regionsKey];
    if(regionsDict == nil) {
        return 0;
    } else {
        return (int)[regionsDict count];
    }
}

@end
  
