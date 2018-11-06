/*
 * Copyright (c) 2011-2016 BlackBerry Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.blackberry.bidlocation;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Constants used in this sample.
 */
public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "com.blackberry.bidlocation";
    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";
    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    /**
     * Geofences expire after 24 hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 24;
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    /**
     * Stored geofences have a radius of 1km
     */
    public static final float GEOFENCE_RADIUS_IN_METERS = 1000; // 1 km

    /**
     * Stored information about default locations
     */
    public static HashMap<String, List<LatLng>> FENCES = new HashMap<String, List<LatLng>>();
    static {
        List<LatLng> defaultLocnCoords = new ArrayList<LatLng>();
        defaultLocnCoords.add(new LatLng(43.5168, -80.5139));
        defaultLocnCoords.add(new LatLng(43.5155, -80.510024));
        FENCES.put("Blackberry Northfield", defaultLocnCoords);

    }
}
