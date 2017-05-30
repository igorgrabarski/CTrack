package igorgrabarski.com.ctrack;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Objects;

public class WorkService extends Service implements android.location.LocationListener {


    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    FirebaseDatabase database;
    TelephonyManager telephonyManager;
    GsmCellLocation gsmCellLocation;

    LocationManager locationManager;
    boolean providerEnabled;


    // Default settings
    private final long INTERVAL = 30000;
    private final long FASTEST_INTERVAL = 15000;

    public WorkService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createLocationRequest();


        //****************************** Set settings from database **************************
        database = FirebaseDatabase.getInstance();
        final DatabaseReference myRefSettings = database.getReference("settings");
        myRefSettings.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mLocationRequest.setInterval(dataSnapshot.child("interval").getValue(Long.class));
                mLocationRequest.setFastestInterval(dataSnapshot.child("fastest_interval").getValue(Long.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //************************************************************************************

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d("casa", "return");
            return START_STICKY;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


        if (providerEnabled) {
            useGPS();
        } else {
            useCellTowers();
        }


        //************* Restart service automatically if destroyed by Android *****************
        return START_STICKY;
    }

    //****************** Disconnects GoogleApiClient before destroy ***************************
    @Override
    public void onDestroy() {

        mGoogleApiClient.disconnect();
    }

    //************************** Settings for Location updates ********************************
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    //******************************************************************************************


    private void useGPS() {

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {

                            if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                    android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                                    PackageManager.PERMISSION_GRANTED &&
                                    ActivityCompat.checkSelfPermission(getApplicationContext(),
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                            PackageManager.PERMISSION_GRANTED) {

                                return;
                            }


                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                                    mLocationRequest,
                                    new LocationListener() {
                                        @Override
                                        public void onLocationChanged(final Location location) {

                                            Log.d("casa", "useGPS");
                                            database = FirebaseDatabase.getInstance();
                                            final DatabaseReference myRef = database.getReference("/");

                                            //********* Create Location object *******************
                                            PersonLocation personLocation = new PersonLocation();
                                            personLocation.setLat(location.getLatitude());
                                            personLocation.setLng(location.getLongitude());
                                            personLocation.setCurrentDateTime(new Date());
                                            personLocation.setCid(-1);
                                            personLocation.setLac(-1);

                                            //********* Update location in database ***************
                                            myRef.child("location").setValue(personLocation);

                                            if (!providerEnabled) {
                                                useCellTowers();
                                            }

                                        }

                                    });
                        }


                        @Override
                        public void onConnectionSuspended(int i) {
                            useCellTowers();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            useCellTowers();
                        }
                    })
                    .addApi(LocationServices.API)
                    .build();
        }

        //*************************** Connect GoogleApiClient on service start ****************
        mGoogleApiClient.connect();


    }

    private void useCellTowers() {
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        gsmCellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

        database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference("/");

//        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        while (!providerEnabled) {

            Log.d("casa", "use cell towers");
            //********* Create Location object *******************
            PersonLocation personLocation = new PersonLocation();
            personLocation.setCurrentDateTime(new Date());
            personLocation.setCid(gsmCellLocation.getCid());
            personLocation.setLac(gsmCellLocation.getLac());
            personLocation.setLat(-1);
            personLocation.setLng(-1);

            //********* Update location in database ***************
            myRef.child("location").setValue(personLocation);


            try {
                Thread.sleep(FASTEST_INTERVAL);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        }

        useGPS();

    }


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {


    }

    @Override
    public void onProviderEnabled(String provider) {
        if (Objects.equals(provider, "gps")) {
            providerEnabled = true;
            useGPS();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (Objects.equals(provider, "gps")) {
            providerEnabled = false;
            useCellTowers();
        }
    }
}
