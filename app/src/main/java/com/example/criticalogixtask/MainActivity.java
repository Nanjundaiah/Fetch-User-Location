package com.example.criticalogixtask;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.audiofx.BassBoost;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{
    Button fetch , stop;
    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 100;
    private Location location;
    private TextView result;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 5000;
    FusedLocationProviderClient client;
    JSONObject jsonObject = new JSONObject();
    private List<DataModel> list = new ArrayList<>();
    DataModel dataModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fetch = findViewById(R.id.fetch_btn);
        stop = findViewById(R.id.stop_btn);
        result = findViewById(R.id.result_tv);
        client = LocationServices.getFusedLocationProviderClient(this);


        fetch.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                stop.setEnabled(true);
                list.clear();
                result.setText("Fetching Your Location...");
                AskPermission();

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopFetching();
            }
        });
    }

    public  void startFetching(){
        Intent serviceIntent = new Intent(this, FetchLocationService.class);
        serviceIntent.putExtra("inputExtra", "Fetching Location");
       ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopFetching() {
        stopService(new Intent(this, FetchLocationService.class));
        if(jsonObject != null){
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(list);
            String json = new Gson().toJson(list);
            result.setText(json);
        }
        stop.setEnabled(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        getCurrentLocation();
        super.onResume();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void AskPermission(){
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
            startFetching();
            handler.postDelayed(runnable = new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                public void run() {
                    handler.postDelayed(runnable, delay);
                    getCurrentLocation();
                }
            }, delay);
        }else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION);
        }

    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
       if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
           || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
           client.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
               @Override
               public void onComplete(@NonNull @NotNull Task<Location> task) {
                   location = task.getResult();
                   if(location != null){

                        dataModel = new DataModel( String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()));
                        list.add(dataModel);
                   }else{
                       LocationRequest locationRequest = new LocationRequest()
                               .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                               .setInterval(10000)
                               .setFastestInterval(1000)
                               .setNumUpdates(1);

                       LocationCallback locationCallback =new LocationCallback() {
                           @Override
                           public void onLocationResult(@NonNull @NotNull LocationResult locationResult) {
                               Location location = locationResult.getLastLocation();
                           }
                       };
                       client.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                   }

               }
           });
       }else{
           startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
       }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_ACCESS_COARSE_LOCATION && (grantResults.length > 0) &&
        grantResults[0]+  grantResults[1] == PackageManager.PERMISSION_GRANTED){
            getCurrentLocation();
        }else{
            Toast.makeText(getApplicationContext(),"Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

}