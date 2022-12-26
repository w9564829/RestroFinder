package com.example.restrofinder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "AIzaSyB_QMemibraOGH-oyJxp-i5qTbn29lRNy8";
    LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private int PROXIMITY_RADIUS = 1000;

    private ListView lvRestaurantList;
    private LinearLayout errLayout;
    private ProgressBar progressBar;
    private LatLng mLastKnownLatLng;
    private RestaurantListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvRestaurantList = findViewById(R.id.lvRestaurantList);
        errLayout = findViewById(R.id.errLayout);
        progressBar = findViewById(R.id.progressBar);

    }

    @Override
    protected void onResume() {
        super.onResume();
        showRestaurants();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_item, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.favoriteRestroMenu:
                intent = new Intent(MainActivity.this, FavRestaurantActivity.class);
                startActivity(intent);
                break;
            case R.id.openMap:
                intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
                break;
            case R.id.logout:
                logout();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public void showRestaurants() {
        progressBar.setVisibility(View.VISIBLE);
        getLocationAndFetchRestaurants();
    }

    private void setRestaurantsDataIntoList(String jsonData) {
        JSONArray jsonArray = convertStringToJsonArray(jsonData);
        List<Restaurant> restaurants = convertJsonArrayToRestaurantsList(jsonArray);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("users").
                document(user.getEmail()).
                collection("Favorite").
                get().
                addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        ArrayList<Restaurant> favoriteRestaurant = new ArrayList<>();
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Restaurant restaurant = document.toObject(Restaurant.class);
                                restaurant.setRestaurantID(document.getId());
                                favoriteRestaurant.add(restaurant);
                            }
                            if (restaurants != null) {
                                adapter = new RestaurantListAdapter(MainActivity.this,
                                        restaurants, mLastKnownLatLng, favoriteRestaurant);
                                lvRestaurantList.setAdapter(adapter);
                                progressBar.setVisibility(View.GONE);
                                errLayout.setVisibility(View.GONE);
                                lvRestaurantList.setVisibility(View.VISIBLE);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                errLayout.setVisibility(View.VISIBLE);
                                lvRestaurantList.setVisibility(View.GONE);
                            }

                        } else {
                            if (restaurants != null) {
                                adapter = new RestaurantListAdapter(MainActivity.this,
                                        restaurants, mLastKnownLatLng, favoriteRestaurant);
                                lvRestaurantList.setAdapter(adapter);
                                progressBar.setVisibility(View.GONE);
                                errLayout.setVisibility(View.GONE);
                                lvRestaurantList.setVisibility(View.VISIBLE);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                errLayout.setVisibility(View.VISIBLE);
                                lvRestaurantList.setVisibility(View.GONE);
                            }
                        }

                    }
                });


    }

    private JSONArray convertStringToJsonArray(String jsonData) {
        JSONArray jsonArray = null;
        JSONObject jsonObject;
        try {
            Log.d("Places", "parse");
            jsonObject = new JSONObject((String) jsonData);
            jsonArray = jsonObject.getJSONArray("results");
        } catch (JSONException e) {
            Log.d("Places", "parse error");
            e.printStackTrace();
        }
        return jsonArray;
    }

    private List<Restaurant> convertJsonArrayToRestaurantsList(JSONArray jsonArray) {
        int placesCount = jsonArray.length();
        List<Restaurant> restaurants = new ArrayList<>();
        Log.d("Places", "getPlaces " + placesCount);

        for (int i = 0; i < placesCount; i++) {
            try {
                Restaurant restaurant = getRestaurant((JSONObject) jsonArray.get(i));
                restaurants.add(restaurant);
                Log.d("restaurants", "Adding restaurant");

            } catch (JSONException e) {
                Log.d("restaurants", "Error in Adding restaurants");
                e.printStackTrace();
            }
        }
        return restaurants;
    }

    private Restaurant getRestaurant(JSONObject googlePlaceJson) {
        Restaurant restaurant = new Restaurant();
        String placeName = "-NA-";
        String vicinity = "-NA-";
        String latitude;
        String longitude;
        String imageUrl;

        Log.d("getPlace", "Entered");

        try {
            if (!googlePlaceJson.isNull("name")) {
                placeName = googlePlaceJson.getString("name");
            }
            if (!googlePlaceJson.isNull("vicinity")) {
                vicinity = googlePlaceJson.getString("vicinity");
            }
            latitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lat");
            longitude = googlePlaceJson.getJSONObject("geometry").getJSONObject("location").getString("lng");
            JSONObject photos = (JSONObject) googlePlaceJson.getJSONArray("photos").get(0);
            imageUrl = photos.getString("photo_reference");

            restaurant.setRestaurantName(placeName);
            restaurant.setAddress(vicinity);
            restaurant.setLatitude(latitude);
            restaurant.setLongitude(longitude);
            restaurant.setImageUrl(generateRestaurantImageUrl(imageUrl));

            Log.d("getPlaceImage", restaurant.getImageUrl());
        } catch (JSONException e) {
            Log.d("getPlace", "Error");
            e.printStackTrace();
        }
        return restaurant;
    }

    private String generateRestaurantImageUrl(String imageReference) {
        String url = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&" +
                "photo_reference=" + imageReference +
                "&key=" + API_KEY;
        return url;
    }

    public void getLocationAndFetchRestaurants() {
        locationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        checkLocationPermission();
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (location == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            checkLocationPermission();
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double lat = location.getLatitude();
                                double longi = location.getLongitude();
                                mLastKnownLatLng = new LatLng(lat, longi);
                                fetchNearbyRestaurantsData(lat, longi);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                errLayout.setVisibility(View.VISIBLE);
                                lvRestaurantList.setVisibility(View.GONE);
                            }
                        }
                    });
        } else {
            mLastKnownLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            fetchNearbyRestaurantsData(location.getLatitude(), location.getLongitude());
        }
    }

    private void fetchNearbyRestaurantsData(double lat, double longi) {
        String url = getUrlForNearbyPlaces(lat, longi, "restaurant");
        RequestQueue mRequestQueue = Volley.newRequestQueue(this);

        StringRequest mStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response != null) {
                    Log.i("NearByPlaces", response);
                    setRestaurantsDataIntoList(response);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressBar.setVisibility(View.GONE);
                errLayout.setVisibility(View.VISIBLE);
                lvRestaurantList.setVisibility(View.GONE);
                Log.i("NearByPlaces", "Error :" + error.toString());
            }
        });

        mRequestQueue.add(mStringRequest);

        Toast.makeText(MainActivity.this, "Showing Nearby Restaurants", Toast.LENGTH_LONG).show();
    }

    private String getUrlForNearbyPlaces(double latitude, double longitude, String nearbyPlace) {
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + PROXIMITY_RADIUS);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + API_KEY);
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        getLocationAndFetchRestaurants();
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

}