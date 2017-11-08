
#import "RNGeofence.h"

@implementation RNGeofence

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()
RCT_EXPORT_METHOD(startMonitoring)
{
    RCTLogInfo(@"Start monitoring geofences");
}
@end
  
