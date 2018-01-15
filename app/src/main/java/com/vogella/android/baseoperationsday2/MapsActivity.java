package com.vogella.android.baseoperationsday2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import java.util.*;
import java.io.*;
import org.json.*;
import org.json.simple.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import java.lang.Long;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient client;
    private LocationRequest locationRequest;
    private LocationManager locationManager;
    private Location lastLocation;
    private JSONArray jsonArray;
    private Marker currentLocationMarker;
    public static final int REQUEST_LOCATION_CODE = 99;
    private double mexicoCityLat = 19.4326, mexicoCityLon = -99.1332;
    private double currentLat, currentLon, radius;
    private boolean hasFocused = false;

    JSONParser parser = new JSONParser();
    InputStream is = null;
    //Read a json file and generate Markers and add a heatmap



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            checkLocationPermission();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        switch(requestCode)
        {
            case REQUEST_LOCATION_CODE:
                if(grantResults.length >0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                {
                    //permission is granted
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)
                    {
                        if(client == null)
                        {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else //permission denied
                {
                    Toast.makeText(this,"Permission denied!",Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();

        try
        {
            is = getAssets().open("example_json_file.json");
            JSONParser jsonParser = new JSONParser();
            jsonArray = (JSONArray) jsonParser.parse(new InputStreamReader(is, "UTF-8"));
            //generateMarkers(jsonArray);o
            Date d1 = new Date(2017,11,28,0,0,0);Date d2 = new Date(2017,11,28,0,8,0);
            generateMarkers(jsonArray,d1,d2);
            addHeatMap(jsonArray);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        catch(ParseException e)
        {
            e.printStackTrace();
        }
    }

    protected synchronized void buildGoogleApiClient()
    {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
        client.connect();
    }

    @Override
    public void onLocationChanged(Location location)
    {
        if (!hasFocused) {
            lastLocation = location;
            if (currentLocationMarker != null) {
                currentLocationMarker.remove();
            }

            currentLat = location.getLatitude();
            currentLon = location.getLongitude();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LatLng displayedLocation;

                boolean outside = Math.max(Math.abs(currentLat - mexicoCityLat), Math.abs(currentLon - mexicoCityLon)) >= radius;
                if (outside) {
                    displayedLocation = new LatLng(mexicoCityLat, mexicoCityLon);
                } else {
                    displayedLocation = new LatLng(currentLat, currentLon);
                    mMap.setMyLocationEnabled(true);
                }
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(displayedLocation)
                        .zoom(14.3f)                   // Sets the zoom
                        .build();                   // Creates a CameraPosition from the builder
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            hasFocused = true;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        //Control the spontaneity of updates
        locationRequest = new LocationRequest();

        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(client,locationRequest,this);
        }

    }

    public boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
            }
            else
            {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else return true;
    }

    public void generateMarkers(JSONArray jsonArray)
    {
        //Generate location (lat,lng)
        JSONArray jsonLocations = parseLocation(jsonArray);
        //Generate marker label
        JSONArray jsonEventLabel = parseEventLabel(jsonArray);
        for(int i=0;i<jsonLocations.size();i++)
        {
            JSONArray jLoc = (JSONArray)jsonLocations.get(i);
            JSONObject jObject0 = (JSONObject)jLoc.get(0);
            JSONObject jObject1 = (JSONObject)jLoc.get(1);
            LatLng loc = new LatLng((Double)jObject0.get("Lat"+i),(Double)jObject1.get("Lon"+i));

            JSONObject jsonLabel = (JSONObject)jsonEventLabel.get(i);
            String sLabel = (String)jsonLabel.get("Event"+i);
            sLabel = sLabel.replaceAll("_"," ");
            //Custom place marker
            Marker marker = mMap.addMarker(new MarkerOptions().position(loc).title("Event Label")
                    .snippet(sLabel).icon(BitmapDescriptorFactory.fromAsset("yellow2.bmp")));
            marker.showInfoWindow();
        }
    }

    public void generateMarkers(JSONArray jsonArray,Date startDate,Date endDate)
    {
        //Generate location (lat,lng)
        JSONArray jsonLocations = parseLocation(jsonArray);
        //Generate marker label
        JSONArray jsonEventLabel = parseEventLabel(jsonArray);
        //Generate event datetime
        JSONArray jsonEventDatetime = parseEventDatetime((jsonArray));
        ArrayList<Date> dateList = buildDates(jsonEventDatetime);

        for(int i=0;i<dateList.size();i++)
        {
            ArrayList<Date> dateRangeList = dateRange(dateList,startDate,endDate);

            if(dateRangeList.contains(dateList.get(i)))
            {
                JSONArray jLoc = (JSONArray)jsonLocations.get(i);
                JSONObject jObject0 = (JSONObject)jLoc.get(0);
                JSONObject jObject1 = (JSONObject)jLoc.get(1);
                LatLng loc = new LatLng((Double)jObject0.get("Lat"+i),(Double)jObject1.get("Lon"+i));

                JSONObject jsonLabel = (JSONObject)jsonEventLabel.get(i);
                String sLabel = (String)jsonLabel.get("Event"+i);
                sLabel = sLabel.replaceAll("_"," ");
                //Custom place marker
                Marker marker = mMap.addMarker(new MarkerOptions().position(loc).title("Event Label")
                        .snippet(sLabel).icon(BitmapDescriptorFactory.fromAsset("yellow2.bmp")));
                marker.showInfoWindow();
            }

        }
    }

    public static JSONArray parseLocation(JSONArray jArray)
    {
        JSONArray ret = new JSONArray();
        for(int i=0;i<jArray.size();i++)
        {
            JSONObject jLoc = (JSONObject)jArray.get(i);
            JSONArray jAry = new JSONArray();
            JSONObject jO0 = new JSONObject();
            JSONObject jO1 = new JSONObject();
            jO0.put("Lat"+i,jLoc.get("Event_Lat"));
            jO1.put("Lon"+i,jLoc.get("Event_Lon"));
            jAry.add(jO0);
            jAry.add(jO1);
            ret.add(jAry);
        }
        return ret;
    }

    public static JSONArray parseEventLabel(JSONArray jArray)
    {
        JSONArray ret = new JSONArray();
        for(int i=0;i<jArray.size();i++)
        {
            JSONObject jEvent = (JSONObject)jArray.get(i);
            JSONObject jO = new JSONObject();
            jO.put("Event"+i,jEvent.get("Event_Label"));
            ret.add(jO);
        }
        return ret;
    }

    private void addHeatMap(JSONArray jsonArray)
    {
        List<WeightedLatLng> list = buildWeightedLatLng(jsonArray);

        // Create the gradient.

        int[] colors = {
                Color.argb(0,200,200, 200),//gray
                Color.rgb(200, 200, 200),//gray
                Color.rgb(0, 100, 200),//blue
                Color.rgb(0, 0, 200),//darker blue
                Color.rgb(200, 0, 200),//lavender
                Color.rgb(100, 0, 100)//purple
        };

        int[]gradColors = new int[colors.length*5];
        float[]startPoints = new float[colors.length*5];
        int index = 0;
        for(int i=0;i<colors.length;i++)
        {
            for(int j=0;j<5;j++)
            {
                gradColors[index++] = colors[i];
            }
        }
        for(int i=0;i<colors.length*5;i++)
        {
            double d = 1.0/5/colors.length*(i+1);
            String s = d+"f";
            startPoints[i] = Float.parseFloat(s);;
        }
        Gradient gradient = new Gradient(gradColors, startPoints);
        // Create the tile provider.
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().weightedData(list).gradient(gradient).radius(50).build();
        // Add the tile overlay to the map.
        TileOverlay mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        /*
        float zoom = mMap.getCameraPosition().zoom;
        if(zoom != 14.3)
        {
            HeatmapTileProvider mProvider1 = new HeatmapTileProvider.Builder().weightedData(list).gradient(gradient).radius((int)Math.random()*40+10).build();
        }
        */
    }

    public List<WeightedLatLng> buildWeightedLatLng(JSONArray jsonArray)
    {
        List<WeightedLatLng> ret = new ArrayList<>();
        for(int i=0;i<jsonArray.size();i++)
        {
            JSONObject j = (JSONObject)jsonArray.get(i);
            LatLng latLng = new LatLng((Double)j.get("Event_Lat"),(Double)j.get("Event_Lon"));
            Long severity = (Long)j.get("Event_Severity");
            WeightedLatLng wLatLng = new WeightedLatLng(latLng,severity.doubleValue());
            ret.add(wLatLng);
        }
        return ret;
    }

    public static JSONArray parseEventSeverity(JSONArray jArray)
    {
        JSONArray ret = new JSONArray();
        for(int i=0;i<jArray.size();i++)
        {
            JSONObject jEvent = (JSONObject)jArray.get(i);
            JSONObject jO = new JSONObject();
            jO.put("Event"+i,jEvent.get("Event_Severity"));
            ret.add(jO);
        }
        return ret;
    }

    public static JSONArray parseEventDatetime(JSONArray jArray)
    {
        JSONArray ret = new JSONArray();
        for(int i=0;i<jArray.size();i++)
        {
            JSONObject jEvent = (JSONObject)jArray.get(i);
            JSONObject jO = new JSONObject();
            jO.put("Event"+i,jEvent.get("Event_Datetime"));
            ret.add(jO);
        }
        return ret;
    }

    public static ArrayList<Date> buildDates(JSONArray jArray)
    {
        ArrayList<Date> dates = new ArrayList<>();
        for(int i=0;i<jArray.size();i++)
        {
            JSONObject jEntireDate = (JSONObject)jArray.get(i);
            String sDate = (String)(jEntireDate.get("Event"+i));
            //System.out.println(sDate);
            sDate = sDate.replaceAll("[ ,:]","").substring(3);
            int date = Integer.parseInt(sDate.substring(0,2)),
                    //month = 0,
                    year = Integer.parseInt(sDate.substring(5,9)),
                    hrs = Integer.parseInt(sDate.substring(9,11)),
                    min = Integer.parseInt(sDate.substring(11,13)),
                    sec = Integer.parseInt(sDate.substring(13,15));

            Map map = new HashMap();
            map.put("Jan",0);
            map.put("Feb",1);
            map.put("Mar",2);
            map.put("Apr",3);
            map.put("May",4);
            map.put("Jun",5);
            map.put("Jul",6);
            map.put("Aug",7);
            map.put("Sep",8);
            map.put("Oct",9);
            map.put("Nov",10);
            map.put("Dec",11);

            dates.add(new Date(year,(int)map.get(sDate.substring(2,5)),date,hrs,min,sec));
        }
        //Collections.sort(dates);
        return dates;
    }

    public static ArrayList<Date> dateRange(ArrayList<Date>dates,Date startDate,Date endDate)
    {
        ArrayList<Date> ret = new ArrayList<>();
        for(int i=0;i<dates.size();i++)
        {
            if(startDate.compareTo(dates.get(i))<=0 && endDate.compareTo(dates.get(i))>=0)
            {
                ret.add(dates.get(i));
            }
        }
        return ret;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
