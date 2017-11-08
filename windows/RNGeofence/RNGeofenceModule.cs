using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace Geofence.RNGeofence
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNGeofenceModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNGeofenceModule"/>.
        /// </summary>
        internal RNGeofenceModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNGeofence";
            }
        }
    }
}
