package com.jumayu.cab;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "DriverMapActivity";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    Location lastLocation;
    private final static int REQUEST_PERMISSION_CODE = 100;
    LocationRequest locationRequest;
    boolean locationPermissionGranted = false;
    LocationManager locationManager;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    Button LogoutDriverButton;
    Button SettingsDriverButton;
    Boolean currentLogoutDriverStatus = false;
    DatabaseReference AssignedCustomerRef, AssignedCustomerPickUpRef;
    String driverId, customerId = "";
    Marker CustomerMarker, PickUpMarker;

    public void findViews() {
        LogoutDriverButton = findViewById(R.id.driver_logout_btn);
        SettingsDriverButton = findViewById(R.id.driver_settings_btn);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        findViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverId = currentUser.getUid();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

//        lmRequestLocationUpdates();

        LogoutDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogoutDriver();
            }
        });

        GetAssignedCustomerRequest();

    }

    private void GetAssignedCustomerRequest() {
        AssignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("CustomerRideId");
        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    customerId = dataSnapshot.getValue().toString();
                    GetAssignedCustomerPickUpLocation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GetAssignedCustomerPickUpLocation() {
        AssignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests").child(customerId).child("l");
        AssignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0, LocationLng = 0;
                    if (customerLocationMap.get(0) != null) {
                        LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if (customerLocationMap.get(1) != null) {
                        LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    LatLng CustomertLatLng = new LatLng(LocationLat, LocationLng);
                    CustomerMarker = mMap.addMarker(new MarkerOptions().position(CustomertLatLng).title("Customer Pickup."));
                    if (CustomerMarker == null) {
                        CustomerMarker.remove();
                    }
                }
                else {
                    customerId="";
                    if(PickUpMarker!=null) {
                        PickUpMarker.remove();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void fetchLastKnownLocation() {
        mMap.setMyLocationEnabled(true);
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    lastLocation = location;
                    Log.d("lastLoc", "" + lastLocation);
                    LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void lmRequestLocationUpdates() {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    lastLocation = location;
                    if (getApplicationContext() != null) {
                        if (!currentLogoutDriverStatus) {
                            //Store Location properties in FireBase DB
                            String UserId = mAuth.getUid();
                            DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
                            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");

                            GeoFire geoFireAvailability = new GeoFire(DriverAvailabilityRef);
                            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);

                            switch (customerId) {
                                case "":
//                                geoFireWorking.removeLocation(UserId);
                                    geoFireWorking.setLocation(UserId, new GeoLocation(location.getLatitude(), location.getLongitude()), new
                                            GeoFire.CompletionListener() {
                                                @Override
                                                public void onComplete(String key, DatabaseError error) {
                                                }
                                            });
                                    break;
                                default:
                                    geoFireAvailability.removeLocation(UserId);
                                    geoFireAvailability.setLocation(UserId, new GeoLocation(location.getLatitude(), location.getLongitude()), new
                                            GeoFire.CompletionListener() {
                                                @Override
                                                public void onComplete(String key, DatabaseError error) {
                                                }
                                            });
                                    break;
                            }
                        }
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });


    }

    public void checkLocationPermissions() {
        Log.d("permission", "checking");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = false;
            // request for permission
            Log.d(TAG, "permission not granted");
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET}, REQUEST_PERMISSION_CODE);

        } else {
            //permission granted
            Log.d(TAG, "permission granted");
            locationPermissionGranted = true;
            lmRequestLocationUpdates();
            fetchLastKnownLocation();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermissions();
        if(locationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            fetchLastKnownLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                checkLocationPermissions();
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (!currentLogoutDriverStatus) {
            DisconnectDriver();
        }
    }

    private void DisconnectDriver() {
        String UserId = mAuth.getUid();
        DatabaseReference DriverAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(UserId);
    }

    public void LogoutDriver() {
        currentLogoutDriverStatus = true;
        mAuth.signOut();
        Intent welcomeIntent = new Intent(DriverMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }
}
