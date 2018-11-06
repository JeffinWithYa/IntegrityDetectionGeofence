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

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.blackberry.bidlocation.bidhelper.BidHelperAndroid;
import com.blackberry.bidlocation.bidhelper.BidListener;
import com.blackberry.bidlocation.bidhelper.BidSignatureVerificationException;
import com.blackberry.bidlocation.bidhelper.BidStatusReport;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Listener for geofence transition changes. Creates a notification and launches a BID report as the
 * output.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition.
 */
public class BidReportOnGeofenceTransitions extends IntentService implements BidListener{

    //BID Variables
    public static final boolean DEBUG = false;
    public static final int GET_BID_REPORT = 1;
    public static final int DEVICE_COMPROMISED = 2;
    public static final int DEVICE_SAFE = 3;
    private static final String TAG = "BIDReportOnTransitions";
    private final HandlerThread mHandlerThread = new HandlerThread("BidLoginActivity", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private BidReportHandler mHandler;
    private Handler mUIHandler;
    private BidHelperAndroid mHelper;
    private Context context;

    public BidReportOnGeofenceTransitions() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        context = this;
        mHelper = new BidHelperAndroid(this);
        mHelper.addBidListener(this);
        super.onCreate();
    }

    @Override
    public void onDestroy(){
        mHelper.destroy();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Check if the reported transition was an enter or exit
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails);
            Log.i(TAG, geofenceTransitionDetails);

            // Ensure that this application is running on a Blackberry Device
            if (Build.MANUFACTURER.equalsIgnoreCase("BlackBerry")) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    getBidReport();
                } else {
                    Toast.makeText(this, R.string.version_msg,
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, R.string.warning_msg, Toast.LENGTH_LONG).show();
            }

        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }
    private void getBidReport() {
        mUIHandler = new Handler() {
            public void handleMessage(Message msg) {
                final int what = msg.what;
                switch (what) {
                    case DEVICE_SAFE:
                        // When device is safe, enable desired functionalities here
                        break;
                    default:
                        break;
                }
            }
        };
        mHandlerThread.start();
        mHandler = new BidReportHandler(mHandlerThread.getLooper());
        mHandler.sendEmptyMessage(GET_BID_REPORT);
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context               The app context.
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.icon)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.icon))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }
    @Override
    public void certificateAvailable() {
        Toast.makeText(this, R.string.cert_available, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void reportInserted() {
        Toast.makeText(this, R.string.report_inserted, Toast.LENGTH_SHORT).show();
    }
    /**
     * It allows for requesting the status report and verifying it.
     * The report generated should be verified before opening.
     */
    private void queryStatus() {
        try {
            BigInteger bi = new BigInteger(256, new Random());
            BidStatusReport report = mHelper.requestStatusReport(bi);
            try {
                mHelper.verifyStatusReport(report, bi, true);
            } catch (BidSignatureVerificationException ex) {
                Log.e(TAG, "verification failed.");
                /**
                 * You can read a particular report without verifying
                 * it by using the below call. THIS IS NOT RECOMMENDED.
                 */
                //report.bypassVerification();
            }
            if (report.hasFailure()) {
                Toast.makeText(context, "Device has been compromised and the severity is: " + report.getMaxSeverity(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Device is safe.", Toast.LENGTH_LONG).show();
                mUIHandler.sendEmptyMessage(DEVICE_SAFE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class BidReportHandler extends Handler {
        public BidReportHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GET_BID_REPORT:
                    //do the processing and get the device state
                    queryStatus();
            }
        }
    }
}
