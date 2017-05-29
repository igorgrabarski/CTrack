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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Date;

public class WorkService extends Service {


    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    FirebaseDatabase database;
    TelephonyManager telephonyManager;
    GsmCellLocation gsmCellLocation;


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

        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        boolean providerEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(providerEnabled){
            useGPS();
        }
        else {
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


    private void useGPS(){
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

                                            database = FirebaseDatabase.getInstance();
                                            final DatabaseReference myRef = database.getReference("/");

                                            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(DataSnapshot dataSnapshot) {

                                                    //********* Create Location object *******************
                                                    PersonLocation personLocation = new PersonLocation();
                                                    personLocation.setLat(location.getLatitude());
                                                    personLocation.setLng(location.getLongitude());
                                                    personLocation.setCurrentDateTime(new Date());
                                                    personLocation.setCid(-1);
                                                    personLocation.setLac(-1);

                                                    //********* Update location in database ***************
                                                    myRef.child("location").setValue(personLocation);
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {

                                                }
                                            });
                                        }
                                    });

                        }

                        @Override
                        public void onConnectionSuspended(int i) {

                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                        }
                    })
                    .addApi(LocationServices.API)
                    .build();
        }

        //*************************** Connect GoogleApiClient on service start ****************
        mGoogleApiClient.connect();

    }

    private void useCellTowers(){
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        gsmCellLocation = (GsmCellLocation) telephonyManager.getCellLocation();

        database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference("/");


        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                //********* Create Location object *******************
                PersonLocation personLocation = new PersonLocation();
                personLocation.setCurrentDateTime(new Date());
                personLocation.setCid(gsmCellLocation.getCid());
                personLocation.setLac(gsmCellLocation.getLac());
                personLocation.setLat(-1);
                personLocation.setLng(-1);

                //********* Update location in database ***************
                myRef.child("location").setValue(personLocation);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
