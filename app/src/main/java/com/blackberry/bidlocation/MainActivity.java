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

import android.Manifest;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.blackberry.bidlocation.AddLocationActivity.RADIUS_OF_EARTH_METERS;
import static com.blackberry.bidlocation.AddLocationActivity.DEFAULT_RADIUS;


/**
 * This sample demonstrates how to launch a Blackberry Integrity Detection (BID) report
 * when the user enters or exits a specified geofence.
 *
 * This sample requires a device's Location settings to be turned on.
 */
public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {

    protected static final String TAG = "MainActivity";

    /**
     * Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;
    private static final int FINE_LOCATION_REQUEST_CODE = 1;

    /**
     * List used to store geofences
     */
    protected ArrayList<Geofence> mGeofenceList;

    /**
     * Used to keep track of whether geofences were added.
     */
    private boolean mGeofencesAdded;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     * Used to persist application state about whether geofences were added.
     */
    private SharedPreferences mSharedPreferences;

    /**
     * Buttons to enable/disable geofences
     */
    private Button mEnableGeofencesButton;
    private Button mDisableGeofencesButton;

    /**
     * For ListView to show active geofences
     */
    ArrayList<String> listOfFences = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        // Get the UI widgets.

        mEnableGeofencesButton = (Button) findViewById(R.id.add_geofences_button);
        mDisableGeofencesButton = (Button) findViewById(R.id.remove_geofences_button);

        adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.textview, listOfFences);
        ListView lv = (ListView) findViewById(R.id.fence_list_view);
        lv.setAdapter(adapter);

        // Request permissions from user (Android 6.0 and above)
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.
                            ACCESS_FINE_LOCATION},
                    FINE_LOCATION_REQUEST_CODE);
        }

        // Initialize list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();

        // Initialize PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME,
                MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
        setButtonsState();

        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList();
        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();


    }

    /**
     * Builds a GoogleApiClient.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * Transition to AddLocationsActivity
     */

    public void launchAddLocationActivity(View view) {
        Intent intent = new Intent(this, AddLocationActivity.class);
        startActivity(intent);
    }

    /**
     * Methods for Option Menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_locations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_menu:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle("About")
                        .setMessage(R.string.about_app)
                        .setPositiveButton("Ok", null)
                        .show();
                return true;

            case R.id.remove_fences_menu:
                Constants.FENCES.clear();
                listOfFences.clear();
                adapter.notifyDataSetChanged();
                mGeofenceList.clear();
                populateGeofenceList();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        populateGeofenceList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
    }

    /**
     * Build GeofencingRequest and specify the list of geofences to be monitored.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // If the device is already inside a fence when the geofence is added, it will trigger
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        if (!mGeofenceList.isEmpty()) {
            builder.addGeofences(mGeofenceList);
            return builder.build();
        }
        return null;
    }

    /**
     * Adds geofences and sets alerts for when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    public void enableGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mGeofenceList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_geofences_in_list),
                    Toast.LENGTH_SHORT).show();
            return;
        } else {

            try {
                LocationServices.GeofencingApi.addGeofences(
                        mGoogleApiClient,
                        getGeofencingRequest(),
                        getGeofencePendingIntent()
                ).setResultCallback(this);
            } catch (SecurityException securityException) {
                // app must use ACCESS_FINE_LOCATION permission.
                Log.e(TAG,
                        getString(R.string.need_access_fine_location),
                        securityException);
            }
        }
    }

    /**
     * Disable geofences, which stops further notifications for when the device enters or exits
     * previously registered geofences.
     */
    public void disableGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent()
            ).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(TAG,
                    getString(R.string.need_access_fine_location),
                    securityException);
        }
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Activity implements the {@link ResultCallback} interface, so this method must be defined.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.apply();

            // Update the UI. Adding geofences enables the 'Disable Geofences' button, and removing
            // geofences enables the 'Enable Geofences' button.
            setButtonsState();
            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, BidReportOnGeofenceTransitions.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Method to convert radius LatLng to distance (meters)
     */
    public static double radius2Meters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    /**
     * Method to find the LatLng of a point on the edge of a circle
     */
    private static LatLng findRadiusLatLng(LatLng center) {
        double radiusAngle = Math.toDegrees(DEFAULT_RADIUS / RADIUS_OF_EARTH_METERS) /
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    public void populateGeofenceList() {
        // no duplicates on ListView
        listOfFences.clear();
        for (Map.Entry<String, List<LatLng>> entry : Constants.FENCES.entrySet()) {
            // Get the radius of the fence in meters.
            float radmeters = (float) radius2Meters(new LatLng(entry.getValue().get(0).latitude,
                            entry.getValue().get(0).longitude),
                    new LatLng(entry.getValue().get(1).latitude,
                            entry.getValue().get(1).longitude));

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence.
                    .setRequestId(entry.getKey())
                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().get(0).latitude,
                            entry.getValue().get(0).longitude,
                            radmeters
                    )

                    // Set the expiration duration of the geofence.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                    // Track entry and exit transitions
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                    // Create the geofence.
                    .build());

            // Populate ListView. Trim Lat/Lon coords to only 9 digits.
            listOfFences.add(entry.getKey() + " at: \n" +
                    entry.getValue().get(0).toString());
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Method to manually add to the geofence list
     */
    public void addToGeofenceList(View view) {

        // Set up UI.
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.manual_locn_prompt, null);
        dialogBuilder.setView(dialogView);

        final EditText placeNameET = (EditText) dialogView.findViewById(R.id.editTextLocnName);
        final EditText latET = (EditText) dialogView.findViewById(R.id.editTextLatitude);
        final EditText lonET = (EditText) dialogView.findViewById(R.id.editTextLongitude);


        // Launch dialog for manual input
        dialogBuilder.setTitle("Make Geofence");
        dialogBuilder.setMessage("Enter Geofence Information Below");
        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                // Check if all fields have been filled
                if (TextUtils.isEmpty(placeNameET.getText()) ||
                        TextUtils.isEmpty(latET.getText()) || TextUtils.isEmpty(lonET.getText())) {
                    Toast.makeText(MainActivity.this, getString(R.string.no_geofences_added_string),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Get fence info and add it to FENCES
                    LatLng centerCoord = new LatLng(Double.parseDouble(latET.getText().toString()),
                            Double.parseDouble(lonET.getText().toString()));
                    List<LatLng> LocnCoords = new ArrayList<LatLng>();
                    LocnCoords.add(centerCoord);
                    LocnCoords.add(findRadiusLatLng(centerCoord));

                    Constants.FENCES.put
                            (placeNameET.getText().toString(),
                                    LocnCoords);
                    populateGeofenceList();
                    Toast.makeText(MainActivity.this, getString(R.string.geofences_added),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
                //do nothing
            }
        });

        AlertDialog newAD = dialogBuilder.create();
        newAD.show();
    }

    /**
     * Ensures that only one button is enabled at any time. The 'Enable Geofences' button is enabled
     * if the user has disabled geofences. The 'Disable Geofences' button is enabled if the
     * user has enabled geofences.
     */
    private void setButtonsState() {
        if (mGeofencesAdded) {
            mEnableGeofencesButton.setEnabled(false);
            mDisableGeofencesButton.setEnabled(true);
        } else {
            mEnableGeofencesButton.setEnabled(true);
            mDisableGeofencesButton.setEnabled(false);
        }
    }
}
