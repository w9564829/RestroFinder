package com.example.restrofinder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.List;

public class FavoriteRestaurantListAdapter extends ArrayAdapter<Restaurant> {

    private List<Restaurant> restaurants;
    private LatLng latLng;
    private Context mContext;
    private int selectedPosition = -1;

    public FavoriteRestaurantListAdapter(Context context, List<Restaurant> restaurants, LatLng latLng) {
        super(context, 0, restaurants);
        this.restaurants = restaurants;
        this.latLng = latLng;
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(mContext).inflate(R.layout.restaurant_list_item, parent, false);
        }

        Restaurant restaurant = restaurants.get(position);

        ImageView ivRestaurant = listItemView.findViewById(R.id.ivRestaurant);
        Glide.with(mContext).load(restaurant.getImageUrl()).into(ivRestaurant);

        TextView nameTxt = listItemView.findViewById(R.id.tvName);
        nameTxt.setText(restaurant.getRestaurantName());

        TextView tvAddress = listItemView.findViewById(R.id.tvAddress);
        tvAddress.setText(restaurant.getAddress());

        restaurant.setDistance(distanceBetweenPoints(Double.parseDouble(restaurant.getLatitude()),
                Double.parseDouble(restaurant.getLongitude()),
                latLng.latitude, latLng.longitude));
        TextView distanceTxt = listItemView.findViewById(R.id.tvDistance);
        distanceTxt.setText("Distance : " + restaurant.getDistance() + " km");


        ImageView ivFavorite = listItemView.findViewById(R.id.ivFavorite);

        if (restaurant.isFavorite()) {
            ivFavorite.setImageResource(R.drawable.ic_favorite);
        } else {
            ivFavorite.setImageResource(R.drawable.ic_favorite_border);
        }

        listItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapsIntent = new Intent(getContext(), MapsActivity.class);
                mapsIntent.putExtra("Restaurant", restaurant);
                mapsIntent.putExtra("Type", "TrackLocation");
                getContext().startActivity(mapsIntent);
            }
        });

        ivFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!restaurant.isFavorite()) {
                    ivFavorite.setImageResource(R.drawable.ic_favorite);
                    restaurant.setFavorite(true);
                    setFavoriteRestaurantToDatabase(restaurant);
                } else {
                    ivFavorite.setImageResource(R.drawable.ic_favorite_border);
                    restaurant.setFavorite(false);
                    removeFavoriteRestaurantFromDatabase(restaurant);
                }
            }
        });

        return listItemView;
    }

    private double distanceBetweenPoints(double lat_a, double lng_a, double lat_b, double lng_b) {
        DecimalFormat formater = new DecimalFormat("#.##");
        double pk = (float) (180.f / Math.PI);

        double a1 = lat_a / pk;
        double a2 = lng_a / pk;
        double b1 = lat_b / pk;
        double b2 = lng_b / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);

        double dis = 6366000 * tt / 1000;

        return Double.parseDouble(formater.format(dis));
    }

    public void setFavoriteRestaurantToDatabase(Restaurant restaurant) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DocumentReference collRef;
        db.collection("users").
                document(user.getEmail()).
                collection("Favorite").
                document().
                set(restaurant).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(mContext, "Restaurant added to favorite", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(mContext, "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void removeFavoriteRestaurantFromDatabase(Restaurant restaurant) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("users").
                document(user.getEmail()).
                collection("Favorite").
                document(restaurant.getRestaurantID()).
                delete().
                addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        restaurants.remove(restaurant);
                        notifyDataSetChanged();
                        Toast.makeText(mContext, "Restaurant removed from favorite", Toast.LENGTH_SHORT).show();
                    }
                });

    }
}
