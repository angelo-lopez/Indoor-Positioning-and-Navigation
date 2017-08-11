package com.angelsoft.angelo_romel.indoorpositioning;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;
import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;
import com.indooratlas.android.sdk.resources.IAFloorPlan;
import com.indooratlas.android.sdk.resources.IALatLng;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;
import com.indooratlas.android.sdk.resources.IAResourceManager;
import com.indooratlas.android.sdk.resources.IAResult;
import com.indooratlas.android.sdk.resources.IAResultCallback;
import com.indooratlas.android.sdk.resources.IATask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import java.io.InputStream;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import java.io.IOException;

import static com.angelsoft.angelo_romel.indoorpositioning.R.id.map;

public class MapsOverlayActivity extends FragmentActivity {

    private static final String TAG = MapsOverlayActivity.class.getSimpleName();
    private static final String REALMTAG = "Realm Database Log:";
    private SharedPreferences pref;

    //current location info
    private String mCurrentFloorPlanId;

    //destination location info
    private ArrayList<Location> mDestinationLocation;
    //current location
    private Location currLocation;
    //joint marker location
    private RealmResults<Location> mJointMarkerResults;
    //destination markers
    private ArrayList<Marker> mDestinationMarker;
    //joint marker markers
    private ArrayList<Marker> mJointMarkerMarker;
    //multiple destinations locations
    private RealmResults<Location> mMultipleDestinationResults;

    //object that will hold realm database default instance.
    private Realm locationRealm;

    private static final float HUE_IABLUE = 200.0f;

    /* used to decide when bitmap should be downscaled */
    private static final int MAX_DIMENSION = 2048;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Marker mMarker;
    private IARegion mOverlayFloorPlan = null;
    private GroundOverlay mGroundOverlay = null;
    private IALocationManager mIALocationManager;
    private IAResourceManager mResourceManager;
    private IATask<IAFloorPlan> mFetchFloorPlanTask;
    private Target mLoadTarget;
    private boolean mCameraPositionNeedsUpdating = true; // update on first location

    //private TextView coordsTextView;
    //private TextView floorIDTextView;
    private TextView destinationTextView;
    private TextView miscInfoTextView;

    /**
     * Listener that handles location change events.
     */
    private IALocationListener mListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */
        @Override
        public void onLocationChanged(IALocation location) {

            //Log.d(TAG, "new location received with coordinates: " + location.getLatitude()
            //        + "," + location.getLongitude());

            //display current coordinates
            //coordsTextView.setText("Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
            //floorIDTextView.setText("Floor ID: " + mCurrentFloorPlanId);
            currLocation = getLocationFromFloorId(mCurrentFloorPlanId);
            if(!pref.getString("destinationId", "none").equalsIgnoreCase("none") && mCurrentFloorPlanId != null) {
                LatLng updatedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                showDestination(currLocation, pref.getString("destinationId", "none"), updatedLatLng);
            }

            if (mMap == null) {
                // location received before map is initialized, ignoring update here
                return;
            }

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (mMarker == null) {
                // first location, add marker
                //mMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                //        .icon(BitmapDescriptorFactory.defaultMarker(HUE_IABLUE)));

                try {
                    mMarker = mMap.addMarker(new MarkerOptions().position(latLng)
                            .icon(BitmapDescriptorFactory.fromResource(getMarkerImageValue("main marker"))));
                }
                catch(Exception e) {
                    Log.d(TAG, "location is null");
                }
            } else {
                // move existing markers position to received location
                mMarker.setPosition(latLng);
            }

            // our camera position needs updating if location has significantly changed
            if (mCameraPositionNeedsUpdating) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.5f));
                mCameraPositionNeedsUpdating = false;
            }
        }
    };

    /**
     * Listener that changes overlay if needed
     */
    private IARegion.Listener mRegionListener = new IARegion.Listener() {

        @Override
        public void onEnterRegion(IARegion region) {
            if (region.getType() == IARegion.TYPE_FLOOR_PLAN) {
                final String newId = region.getId();
                mCurrentFloorPlanId = region.getId();
                // Are we entering a new floor plan or coming back the floor plan we just left?
                if (mGroundOverlay == null || !region.equals(mOverlayFloorPlan)) {
                    mCameraPositionNeedsUpdating = true; // entering new fp, need to move camera
                    if (mGroundOverlay != null) {
                        mGroundOverlay.remove();
                        mGroundOverlay = null;
                    }
                    mOverlayFloorPlan = region; // overlay will be this (unless error in loading)
                    fetchFloorPlan(newId);
                } else {
                    mGroundOverlay.setTransparency(0.0f);
                }
                flashDestinationText();
            }
        }

        @Override
        public void onExitRegion(IARegion region) {
            if (mGroundOverlay != null) {
                // Indicate we left this floor plan but leave it there for reference
                // If we enter another floor plan, this one will be removed and another one loaded
                mGroundOverlay.setTransparency(0.5f);
            }
        }

    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        mDestinationMarker = new ArrayList<Marker>();
        mDestinationLocation = new ArrayList<Location>();
        mJointMarkerMarker = new ArrayList<Marker>();

        // prevent the screen going to sleep while app is on foreground
        findViewById(android.R.id.content).setKeepScreenOn(true);

        // instantiate IALocationManager and IAResourceManager
        mIALocationManager = IALocationManager.create(this);
        mResourceManager = IAResourceManager.create(this);

        //instantiate TextView that displays the current coordinates
        //coordsTextView = (TextView)findViewById(R.id.coords_textview);
        //instantiate TextView that displays the current floor plan id
        //floorIDTextView = (TextView)findViewById(R.id.floor_id_textview);
        //instantiate TextView that displays info on destination
        destinationTextView = (TextView)findViewById(R.id.destination_textview);
        //instantiate TextView that displays additional info on destination
        miscInfoTextView = (TextView)findViewById(R.id.misc_info_textview);

        Realm.init(this);
        RealmConfiguration realmConfig = new RealmConfiguration.Builder().build();
        Realm.deleteRealm(realmConfig);
        locationRealm = Realm.getInstance(realmConfig);
        try {
            loadLocations();
        }
        catch(IOException e) {
            Toast.makeText(this, getString(R.string.load_from_json_error), Toast.LENGTH_SHORT).show();
        }

        FloatingActionButton searchFab = (FloatingActionButton)findViewById(R.id.search_fab);
        searchFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchIntent = new Intent(MapsOverlayActivity.this, SearchActivity.class);
                startActivity(searchIntent);
            }
        });

        FloatingActionButton clearFab = (FloatingActionButton)findViewById(R.id.clear_fab);
        clearFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllDestinationInfo();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //close realm database
        locationRealm.close();
        // remember to clean up after ourselves
        mIALocationManager.destroy();
        pref.edit().putString("destinationId", "none").apply();
        mDestinationLocation = null;
        mDestinationMarker = null;
        mJointMarkerResults = null;
        mJointMarkerMarker = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(map))
                    .getMap();
        }

        // start receiving location updates & monitor region changes
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister location & region changes
        mIALocationManager.removeLocationUpdates(mListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }


    /**
     * Sets bitmap of floor plan as ground overlay on Google Maps
     */
    private void setupGroundOverlay(IAFloorPlan floorPlan, Bitmap bitmap) {

        if (mGroundOverlay != null) {
            mGroundOverlay.remove();
        }

        if (mMap != null) {
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
            IALatLng iaLatLng = floorPlan.getCenter();
            LatLng center = new LatLng(iaLatLng.latitude, iaLatLng.longitude);
            GroundOverlayOptions fpOverlay = new GroundOverlayOptions()
                    .image(bitmapDescriptor)
                    .position(center, floorPlan.getWidthMeters(), floorPlan.getHeightMeters())
                    .bearing(floorPlan.getBearing());

            mGroundOverlay = mMap.addGroundOverlay(fpOverlay);
        }
    }

    /**
     * Download floor plan using Picasso library.
     */
    private void fetchFloorPlanBitmap(final IAFloorPlan floorPlan) {

        final String url = floorPlan.getUrl();

        if (mLoadTarget == null) {
            mLoadTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    Log.d(TAG, "onBitmap loaded with dimensions: " + bitmap.getWidth() + "x"
                            + bitmap.getHeight());
                    setupGroundOverlay(floorPlan, bitmap);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // N/A
                }

                @Override
                public void onBitmapFailed(Drawable placeHolderDraweble) {
                    Toast.makeText(MapsOverlayActivity.this, "Failed to load bitmap",
                            Toast.LENGTH_SHORT).show();
                    mOverlayFloorPlan = null;
                }
            };
        }

        RequestCreator request = Picasso.with(this).load(url);

        final int bitmapWidth = floorPlan.getBitmapWidth();
        final int bitmapHeight = floorPlan.getBitmapHeight();

        if (bitmapHeight > MAX_DIMENSION) {
            request.resize(0, MAX_DIMENSION);
        } else if (bitmapWidth > MAX_DIMENSION) {
            request.resize(MAX_DIMENSION, 0);
        }

        request.into(mLoadTarget);
    }


    /**
     * Fetches floor plan data from IndoorAtlas server.
     */
    private void fetchFloorPlan(String id) {

        // if there is already running task, cancel it
        cancelPendingNetworkCalls();

        final IATask<IAFloorPlan> task = mResourceManager.fetchFloorPlanWithId(id);

        task.setCallback(new IAResultCallback<IAFloorPlan>() {

            @Override
            public void onResult(IAResult<IAFloorPlan> result) {

                if (result.isSuccess() && result.getResult() != null) {
                    // retrieve bitmap for this floor plan metadata
                    fetchFloorPlanBitmap(result.getResult());
                } else {
                    // ignore errors if this task was already canceled
                    if (!task.isCancelled()) {
                        // do something with error
                        Toast.makeText(MapsOverlayActivity.this,
                                "loading floor plan failed: " + result.getError(), Toast.LENGTH_LONG)
                                .show();
                        mOverlayFloorPlan = null;
                    }
                }
            }
        }, Looper.getMainLooper()); // deliver callbacks using main looper

        // keep reference to task so that it can be canceled if needed
        mFetchFloorPlanTask = task;

    }

    /**
     * Helper method to cancel current task if any.
     */
    private void cancelPendingNetworkCalls() {
        if (mFetchFloorPlanTask != null && !mFetchFloorPlanTask.isCancelled()) {
            mFetchFloorPlanTask.cancel();
        }
    }

    private void loadLocations() throws IOException{
        InputStream stream = getAssets().open("locations.json");
        locationRealm.beginTransaction();
        try {
            locationRealm.createAllFromJson(Location.class, stream);
            locationRealm.commitTransaction();
            /*load database into a list and iterate for testing
            List<Location> location = locationRealm.where(Location.class).findAll();
            ListIterator<Location> lt = location.listIterator();
            while(lt.hasNext()) {
                dbElementCount ++;
                Log.d(REALMTAG, lt.next().getId());
            }
            Log.d(REALMTAG, String.valueOf(dbElementCount));
            */
        }
        catch(IOException e) {
            locationRealm.cancelTransaction();
            Log.d(REALMTAG, e.toString());
        }
        finally {
            if(stream != null) {
                stream.close();
            }
        }
    }

    private void showDestination(Location currLocation, String destinationId, LatLng updatedLatLng) {
        String miscDestInfo = "";
        if (destinationId.equalsIgnoreCase("toilet") ||
                destinationId.equalsIgnoreCase("lift") ||
                destinationId.equalsIgnoreCase("staircase") ||
                destinationId.equalsIgnoreCase("emergency exit")) {
            mMultipleDestinationResults = locationRealm.where(Location.class)
                    .equalTo("floorId", currLocation.getFloorId())
                    .equalTo("type", destinationId.toLowerCase())
                    .findAll();
            Log.d(REALMTAG, destinationId + " destination results: " + mMultipleDestinationResults.size());
            if(mMultipleDestinationResults.size() > 0) {
                clearCurrentDestinationInfo();
                destinationTextView.setText("Destination: " + destinationId.toUpperCase());
                miscDestInfo += "- There are " + mMultipleDestinationResults.size() + " " +
                        destinationId + "/s located on the current floor/level.";

                for(int i = 0; i < mMultipleDestinationResults.size(); i ++) {
                    LatLng latLng = new LatLng(mMultipleDestinationResults.get(i).getLat(),
                            mMultipleDestinationResults.get(i).getLon());
                    addMapMarker(latLng, destinationId.toLowerCase());
                }
            }
            else {
                miscDestInfo += "- There are no " + destinationId + "/s located on the current floor/level";
            }
            miscInfoTextView.setText(miscDestInfo);
        }
        else {
            if (isValidLocation(destinationId)) {
                clearCurrentDestinationInfo();
                //add destination to arraylist of locations.
                mDestinationLocation.add(locationRealm.where(Location.class)
                        .equalTo("id", destinationId.toUpperCase())
                        .findFirst());
                miscDestInfo = "- Your destination is";
                for (int i = 0; i < mDestinationLocation.size(); i++) {

                    destinationTextView.setText("Destination: " +
                            mDestinationLocation.get(i).getId().toUpperCase() +
                            ", " + mDestinationLocation.get(i).getBuildingName() +
                            ", " + mDestinationLocation.get(i).getBlock() + " Block, " +
                            "Level " + mDestinationLocation.get(i).getLevel());

                    //destination is on the same building
                    if (mDestinationLocation.get(i).getBuildingName().equalsIgnoreCase(currLocation.getBuildingName())) {
                        if (mDestinationLocation.get(i).getFloorId().equalsIgnoreCase(currLocation.getFloorId())) {
                            //destination is on the same floor/level
                            miscDestInfo += " located on the current floor/level.";
                            LatLng latLng = new LatLng(mDestinationLocation.get(i).getLat(),
                                    mDestinationLocation.get(i).getLon());
                            try {
                                for (int j = 0; j < mDestinationLocation.size(); j++) {
                                    addMapMarker(latLng, mDestinationLocation.get(i).getType());

                                }
                            } catch (Exception e) {
                                Log.d(TAG, e.toString());
                            }
                        } else {
                            //destination is on another level/floor of the same building
                            if ((mDestinationLocation.get(i).getLevel() - currLocation.getLevel()) < 0) {
                                //destination is on a level below current location
                                miscDestInfo += " located "
                                        + Math.abs((mDestinationLocation.get(i).getLevel() - currLocation.getLevel()))
                                        + " level/s below you.\n- Please use the stairs.";
                            } else {
                                //destination is on a level above current location
                                miscDestInfo += " located "
                                        + (mDestinationLocation.get(i).getLevel() - currLocation.getLevel())
                                        + " level/s above you.\n- Please use the stairs.";
                            }
                            Location stairLocation = locationRealm.where(Location.class).equalTo("floorId",
                                    currLocation.getFloorId()).equalTo("type", "staircase").findFirst();
                            if (stairLocation != null) {
                                LatLng latLng = new LatLng(stairLocation.getLat(), stairLocation.getLon());
                                addMapMarker(latLng, stairLocation.getType());
                            }
                            stairLocation = null;//
                        }
                    } else {
                        //destination is on a different building
                    }

                    //Display the distance (in meters) between current location and destination
                    LatLng destinationLatLng = new LatLng(mDestinationLocation.get(i).getLat(),
                            mDestinationLocation.get(i).getLon());
                    miscDestInfo += "\n- Distance is " + getDistanceBetween(updatedLatLng, destinationLatLng) +
                            " meters approximately.";
                    miscInfoTextView.setText(miscDestInfo);

                }//end for
            } else {
                //Toast.makeText(MapsOverlayActivity.this, getString(R.string.destination_not_recognized),
                //        Toast.LENGTH_SHORT).show();
                Log.d(TAG, getString(R.string.destination_not_recognized));
            }
        }
    }

    private boolean isValidLocation(String destinationId) {
        Location location = locationRealm.where(Location.class).equalTo("id", destinationId.toUpperCase()).findFirst();
        if(location == null) {
            return false;
        }
        else {
            return true;
        }
    }

    //clear map data on destination change
    private void clearAllDestinationInfo() {
        destinationTextView.setText("Destination info: ---");
        miscInfoTextView.setText("---");
        pref.edit().putString("destinationId", "none").apply();
        if(mDestinationLocation != null) {
            mDestinationLocation.clear();//clear arraylist
        }
        if(mDestinationMarker != null) {
            for(int i = 0; i < mDestinationMarker.size(); i ++) {
                mDestinationMarker.get(i).remove();//remove marker from map
            }
            mDestinationMarker.clear();//clear arraylist
        }
        if(mJointMarkerMarker != null) {
            for(int i = 0; i <mJointMarkerMarker.size(); i ++) {
                mJointMarkerMarker.get(i).remove();
            }
            mJointMarkerMarker.clear();
        }
        mJointMarkerResults = null;
    }

    //clear map data on floor change
    private void clearCurrentDestinationInfo() {
        if(mDestinationMarker != null) {
            for(int i = 0; i < mDestinationMarker.size(); i ++) {
                mDestinationMarker.get(i).remove();//remove marker from map
            }
            if(mDestinationLocation != null) {
                mDestinationLocation.clear();//clear arraylist
            }
            for(int i = 0; i <mJointMarkerMarker.size(); i ++) {
                mJointMarkerMarker.get(i).remove();//remove joint markers from map
            }
            mJointMarkerResults = null;
            mDestinationMarker.clear();//clear arraylist
            //miscInfoTextView.setText("Additional info: ---");
        }
    }

    private Location getLocationFromFloorId(String floorId) {
        Location currLocation = locationRealm.where(Location.class)
                .equalTo("floorId", floorId)
                .findFirst();
        if(currLocation != null) {
            return currLocation;
        }
        else {
            Log.d(TAG, "getLocationFromFloorId() returned NULL.");
            return null;
        }
    }

    private void flashDestinationText() {
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(100);
        anim.setStartOffset(20);
        anim.setRepeatCount(15);
        //anim.setRepeatMode(Animation.REVERSE);
        //anim.setRepeatCount(Animation.INFINITE);
        miscInfoTextView.startAnimation(anim);
    }

    private void addMapMarker(LatLng latLng, String markerType) {
        mDestinationMarker.add(mMap.addMarker(new MarkerOptions().position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(
                        getMarkerImageValue(markerType)))));
    }

    private int getMarkerImageValue(String locationType) {
        int imageRes = R.drawable.ic_main_marker;
        switch(locationType) {
            case "main marker":
                imageRes = R.drawable.ic_main_marker;
                break;
            case "computer lab":
                imageRes = R.drawable.ic_comp_lab_marker;
                break;
            case "room":
                imageRes = R.drawable.ic_room_marker;
                break;
            case "toilet":
                imageRes = R.drawable.ic_toilet_marker;
                break;
            case "lift":
                imageRes = R.drawable.ic_lift_marker;
                break;
            case "staircase":
                imageRes = R.drawable.ic_staircase_marker;
                break;
            case "emergency exit":
                imageRes = R.drawable.ic_exit_marker;
                break;
        }
        return imageRes;
    }

    private double getDistanceBetween(LatLng origin, LatLng destination) {
        return SphericalUtil.computeDistanceBetween(origin, destination);
    }

}
