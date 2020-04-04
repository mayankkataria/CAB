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
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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

import java.util.HashMap;
import java.util.List;

public class CustomersMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    Location lastLocation;
    private final static int REQUEST_PERMISSION_CODE = 100;
    LocationRequest locationRequest;
    boolean locationPermissionGranted = false, requestType=false;
    LocationManager locationManager;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String customerID, driverFoundID;
    Button LogoutCustomerButton;
    Button SettingsCustomerButton;
    Button CallCabCarButton;
    double radius=0;
    DatabaseReference CustomerDatabaseRef, DriverAvailableRef, DriversRef, DriverLocationRef;
    LatLng CustomerPickupLocation;
    Boolean currentLogoutCustomerStatus=false,  driverFound = false;
    Marker DriverMarker, PickUpMarker;
    ValueEventListener DriverLocationRefListener;
    GeoQuery geoQuery;


    public void init(){
        LogoutCustomerButton=findViewById(R.id.customer_logout_btn);
        SettingsCustomerButton=findViewById(R.id.customer_settings_btn);
        CallCabCarButton=findViewById(R.id.customers_call_cab_button);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mAuth=FirebaseAuth.getInstance();
        currentUser=mAuth.getCurrentUser();
        CustomerDatabaseRef=FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        DriverAvailableRef=FirebaseDatabase.getInstance().getReference().child("Drivers Available");
        DriverLocationRef=FirebaseDatabase.getInstance().getReference().child("Drivers Working");

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_map);

        init();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        LogoutCustomerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogoutCustomer();
            }
        });

        CallCabCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType) {
                    requestType=false;
                    geoQuery.removeAllListeners();
                    DriverLocationRef.removeEventListener(DriverLocationRefListener);

                    if(driverFound != null) {
                        DriversRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Driver").child(driverFoundID);
                        DriversRef.setValue(true);
                        driverFoundID=null;
                    }
                    driverFound=false;
                    radius=1;

                    customerID=currentUser.getUid();
                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()),new
                            GeoFire.CompletionListener(){
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    //Do some stuff if you want to
                                }
                            });
                    geoFire.removeLocation(customerID);

                    if(PickUpMarker != null) {
                        PickUpMarker.remove();
                    }
                    CallCabCarButton.setText("Call a Cab");
                }
                else {
                    requestType=true;
                    customerID=currentUser.getUid();
                    GeoFire geoFire = new GeoFire(CustomerDatabaseRef);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()),new
                            GeoFire.CompletionListener(){
                                @Override
                                public void onComplete(String key, DatabaseError error) {
                                    //Do some stuff if you want to
                                }
                            });
                    geoFire.removeLocation(customerID);
                    CustomerPickupLocation=new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(CustomerPickupLocation).title("Pick up Customer from here"));

                    GetClosestDriverCab();
                }
            }
        });

    }

    public void GetClosestDriverCab() {
        GeoFire geoFire = new GeoFire(DriverLocationRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPickupLocation.latitude, CustomerPickupLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType)
                {
                    Log.d("driver", "found.");
                    driverFound=true;
                    driverFoundID=key;

                    DriversRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers");
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideId", customerID);
                    DriversRef.updateChildren(driverMap);

                    CallCabCarButton.setText("Looking for driver...");
                    GettingDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound)
                {
                    Log.d("driver", "not found. Radius="+radius);
                    radius+=1;
                    GetClosestDriverCab();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingDriverLocation() {
        DriverLocationRefListener=DriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && requestType) {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat=0, LocationLng=0;

                            if(driverLocationMap.get(0) != null) {
                                LocationLat=Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if(driverLocationMap.get(1) != null) {
                                LocationLat=Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                            DriverMarker=mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Your Driver is here."));
                            if(DriverMarker==null) {
                                DriverMarker.remove();
                            }
                            Location location1= new Location("");
                            location1.setLatitude(CustomerPickupLocation.latitude);
                            location1.setLongitude(CustomerPickupLocation.longitude);

                            Location location2= new Location("");
                            location2.setLatitude(LocationLat);
                            location2.setLongitude(LocationLng);

                            float Distance = location1.distanceTo(location2);
                            CallCabCarButton.setText("Driver Found. Distance=" + Distance);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void fetchLastKnownLocation() {
        mMap.setMyLocationEnabled(true);
        Log.d("In fetch", "In fetch");
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location)
            {
                if(location!=null){
                    lastLocation=location;
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
                Log.d("In onLocChange", "In onLocChange");
                lastLocation=location;
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

    private void checkLocationPermissions() {
        // check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted=false;
            // request for permission
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET}, REQUEST_PERMISSION_CODE);

        }
        else {
            //permission granted
            locationPermissionGranted=true;
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
        if(requestCode==REQUEST_PERMISSION_CODE){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                checkLocationPermissions();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(!currentLogoutCustomerStatus)
        {
            DisconnectCustomer();
        }
    }

    private void DisconnectCustomer() {
        String UserId = mAuth.getUid();
        DatabaseReference CustomerAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("Customers Available");
        GeoFire geoFire = new GeoFire(CustomerAvailabilityRef);
        geoFire.removeLocation(UserId);
    }

    public void LogoutCustomer() {
        currentLogoutCustomerStatus=true;
        mAuth.signOut();
        Intent welcomeIntent = new Intent(CustomersMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }
}
