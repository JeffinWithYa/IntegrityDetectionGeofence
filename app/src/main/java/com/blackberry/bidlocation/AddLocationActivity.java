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

import android.content.DialogInterface;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.GoogleMap.OnCircleClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Marker;

import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;

import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddLocationActivity extends AppCompatActivity implements OnMapReadyCallback,
        OnMarkerDragListener, OnMapLongClickListener {
    private static final LatLng WATERLOO = new LatLng(43.5168, -80.5139);
    public static final double DEFAULT_RADIUS = 1000000;
    public static final double RADIUS_OF_EARTH_METERS = 6371009;

    private GoogleMap mMap;

    private List<DraggableCircle> mCircles = new ArrayList<DraggableCircle>(1);
    private List<DraggableCircle> newCircles = new ArrayList<>();

    private int mStrokeColor;
    private int mFillColor;


    private class DraggableCircle {
        private final Marker centerMarker;
        private final Marker radiusMarker;
        private final Circle circle;
        private double radius;

        public DraggableCircle(LatLng center, double radius, boolean clickable) {
            this.radius = radius;
            centerMarker = mMap.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(true));
            radiusMarker = mMap.addMarker(new MarkerOptions()
                    .position(toRadiusLatLng(center, radius))
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_BLUE)));
            circle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeWidth(10)
                    .strokeColor(mStrokeColor) //black
                    .fillColor(mFillColor)
                    .clickable(clickable));
        }

        public DraggableCircle(LatLng center, LatLng radiusLatLng, boolean clickable) {
            this.radius = toRadiusMeters(center, radiusLatLng);
            centerMarker = mMap.addMarker(new MarkerOptions()
                    .position(center)
                    .draggable(true));
            radiusMarker = mMap.addMarker(new MarkerOptions()
                    .position(radiusLatLng)
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_BLUE)));
            circle = mMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(radius)
                    .strokeWidth(10)
                    .strokeColor(mStrokeColor) //black
                    .fillColor(mFillColor)
                    .clickable(clickable));
        }

        public boolean onMarkerMoved(Marker marker) {
            if (marker.equals(centerMarker)) {
                circle.setCenter(marker.getPosition());
                radiusMarker.setPosition(toRadiusLatLng(marker.getPosition(), radius));
                return true;
            }
            if (marker.equals(radiusMarker)) {
                radius = toRadiusMeters(centerMarker.getPosition(), radiusMarker.getPosition());
                circle.setRadius(radius);
                return true;
            }
            return false;
        }

    }

    /* Generate LatLng of radius marker */
    private static LatLng toRadiusLatLng(LatLng center, double radius) {
        double radiusAngle = Math.toDegrees(radius / RADIUS_OF_EARTH_METERS) /
                Math.cos(Math.toRadians(center.latitude));
        return new LatLng(center.latitude, center.longitude + radiusAngle);
    }

    /* Generate distance from center to radius marker */

    private static double toRadiusMeters(LatLng center, LatLng radius) {
        float[] result = new float[1];
        Location.distanceBetween(center.latitude, center.longitude,
                radius.latitude, radius.longitude, result);
        return result[0];
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    /**
     * Manipulates the map once available.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Override the default content description on the view, for accessibility mode.
        googleMap.setContentDescription(getString(R.string.map_circle_description));

        mMap.setOnMarkerDragListener(this);
        mMap.setOnMapLongClickListener(this);

        mFillColor = Color.HSVToColor(127
                , new float[]{1f, 1f, 1f});

        mStrokeColor = Color.BLACK;

        // Set default position of map to Blackberry Northfield
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(WATERLOO, 4.0f));

        googleMap.setOnCircleClickListener(new OnCircleClickListener() {
            @Override
            public void onCircleClick(Circle circle) {
                int strokeColor = circle.getStrokeColor() ^ 0x00ffffff;
                circle.setStrokeColor(strokeColor);
            }
        });

        for (Map.Entry<String, List<LatLng>> entry : Constants.FENCES.entrySet()){
            int index = 0;
            DraggableCircle circle = new DraggableCircle
                    (entry.getValue().get(index), entry.getValue().get(index + 1), true);
            mCircles.add(circle);

        }
        newCircles.clear();

    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        onMarkerMoved(marker);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        onMarkerMoved(marker);
    }

    private void onMarkerMoved(Marker marker) {
        for (DraggableCircle draggableCircle : mCircles) {
            if (draggableCircle.onMarkerMoved(marker)) {
                break;
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng point) {
        // Place outline of fence at a point 3/4 along the view.
        View view = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getView();
        LatLng radiusLatLng = mMap.getProjection().fromScreenLocation(new Point(
                view.getHeight() * 3 / 4, view.getWidth() * 3 / 4));

        // Create circle on map.
        DraggableCircle circle =
                new DraggableCircle(point, radiusLatLng, true);
        mCircles.add(circle);
        newCircles.add(circle);
    }


    public void addMapFences(View view) {

        // Launch dialog to get the name of each new fence drawn on the map.
        for (final DraggableCircle circle : newCircles) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.name_of_fence_dialog, null);
            dialogBuilder.setView(dialogView);
            dialogBuilder.setTitle(getString(R.string.make_geofence_string));
            dialogBuilder.setMessage
                    (getString(R.string.enter_name_for_locn_string)
                            + circle.centerMarker.getPosition().toString());

            // Get name of fence from EditText.
            final EditText fenceNameET = (EditText) dialogView.findViewById(R.id.editTextFenceName);

            // 'ok' button for dialog
            dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int button) {
                    if (TextUtils.isEmpty(fenceNameET.getText())) {
                        Toast.makeText(AddLocationActivity.this,
                                getString(R.string.no_geofences_added_string),
                                Toast.LENGTH_SHORT).show();
                    }

                    else {
                            List<LatLng> LocnCoords = new ArrayList<LatLng>();
                            LocnCoords.add(circle.centerMarker.getPosition());
                            LocnCoords.add(circle.radiusMarker.getPosition());


                            Constants.FENCES.put
                                    (fenceNameET.getText().toString(),
                                            LocnCoords);
                            Toast.makeText(AddLocationActivity.this,
                                    getString(R.string.geofences_added_to_fences_list),
                                    Toast.LENGTH_SHORT).show();

                        }
                    }
                });


            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int button) {
                    Toast.makeText(AddLocationActivity.this,
                            getString(R.string.no_geofences_added_string),
                            Toast.LENGTH_SHORT).show();
                }
            });

            AlertDialog newAD = dialogBuilder.create();
            newAD.show();
        }
    }


    public void launchInfoDialog(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle(getString(R.string.how_to))
                .setMessage(R.string.draw_geofence)
                .setPositiveButton("Ok", null)
                .show();
    }
}
