package com.example.mygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.data.tlog.TLogParser;
import com.o3dr.android.client.utils.video.DecoderListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.o3dr.android.client.apis.ExperimentalApi.getApi;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap mNaverMap;
    private int DEFAULT_ZOOM_LEVEL = 17;
    LatLng DEFAULT_LATLNG = new LatLng(35.9436,126.6842);

    private static final String TAG = MainActivity.class.getSimpleName();

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;

    private Spinner modeSelector;

    private Button startVideoStream;
    private Button stopVideoStream;

    private Button startVideoStreamUsingObserver;
    private Button stopVideoStreamUsingObserver;

    private MediaCodecManager mediaCodecManager;

    private TextureView videoView;

    private String videoTag = "testvideotag";

    Handler mainHandler;

    LocationOverlay locationOverlay;

    ArrayList<String> recycler_list = new ArrayList<>(); // 리사이클러뷰
    ArrayList<LatLng> coords = new ArrayList<>();
    PolylineOverlay polyline = new PolylineOverlay();
    PolylineOverlay polyleadline = new PolylineOverlay();
    private Marker marker = new Marker();

    private double takeoffAltitude = 1.0;
    private LatLong point;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Button plusButton = (Button) findViewById(R.id.plusButton);
        Button minusButton = (Button) findViewById(R.id.minusButton);
        Button lockButton = (Button) findViewById(R.id.lockButton);
        Button moveButton = (Button) findViewById(R.id.moveButton);

        if(plusButton.getVisibility() == View.VISIBLE)
        {
            plusButton.setVisibility(View.INVISIBLE);
            minusButton.setVisibility(View.INVISIBLE);
        }
        if(lockButton.getVisibility() == View.VISIBLE)
        {
            lockButton.setVisibility(View.INVISIBLE);
            moveButton.setVisibility(View.INVISIBLE);
        }

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                Log.d("myCheck", "parent 체크 : " + parent);
                TextView txtView = (TextView)parent.getChildAt(0);
                try {
                    txtView.setTextColor(Color.WHITE);
                } catch (Exception e) {
                    Log.d("myCheck", "예외처리 : " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        FragmentManager fm = getSupportFragmentManager();

        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (mapFragment == null)
        {
            mapFragment =  MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);


        /*=========================================================================================
        final Button takePic = (Button) findViewById(R.id.take_photo_button);
        takePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        final Button toggleVideo = (Button) findViewById(R.id.toggle_video_recording);
        toggleVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleVideoRecording();
            }
        });

        videoView = (TextureView) findViewById(R.id.video_content);
        videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                alertUser("Video display is available.");
                startVideoStream.setEnabled(true);
                startVideoStreamUsingObserver.setEnabled(true);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                startVideoStream.setEnabled(false);
                startVideoStreamUsingObserver.setEnabled(false);
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        startVideoStream = (Button) findViewById(R.id.start_video_stream);
        startVideoStream.setEnabled(false);
        startVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Starting video stream.");
                startVideoStream(new Surface(videoView.getSurfaceTexture()));
            }
        });

        stopVideoStream = (Button) findViewById(R.id.stop_video_stream);
        stopVideoStream.setEnabled(false);
        stopVideoStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Stopping video stream.");
                stopVideoStream();
            }
        });

        startVideoStreamUsingObserver = (Button) findViewById(R.id.start_video_stream_using_observer);
        startVideoStreamUsingObserver.setEnabled(false);
        startVideoStreamUsingObserver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Starting video stream using observer for video stream packets.");
                startVideoStreamForObserver();
            }
        });

        stopVideoStreamUsingObserver = (Button) findViewById(R.id.stop_video_stream_using_observer);
        stopVideoStreamUsingObserver.setEnabled(false);
        stopVideoStreamUsingObserver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertUser("Stopping video stream using observer for video stream packets.");
                stopVideoStreamForObserver();
            }
        });*/

        // Initialize media codec manager to decode video stream packets.
        HandlerThread mediaCodecHandlerThread = new HandlerThread("MediaCodecHandlerThread");
        mediaCodecHandlerThread.start();
        Handler mediaCodecHandler = new Handler(mediaCodecHandlerThread.getLooper());
        mediaCodecManager = new MediaCodecManager(mediaCodecHandler);

        mainHandler = new Handler(getApplicationContext().getMainLooper());
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        mNaverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        final ToggleButton toggleButton2 = (ToggleButton) findViewById(R.id.toggleButton2);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (position == 0) {
                    Toast.makeText(getApplicationContext(), "일반지도", Toast.LENGTH_SHORT).show();
                    mNaverMap.setMapType(NaverMap.MapType.Basic);
                } else if (position == 1) {
                    mNaverMap.setMapType(NaverMap.MapType.Terrain);
                    Toast.makeText(getApplicationContext(), "지형도", Toast.LENGTH_SHORT).show();
                } else if (position == 2) {
                    mNaverMap.setMapType(NaverMap.MapType.Hybrid);
                    Toast.makeText(getApplicationContext(), "위성지도", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

        toggleButton2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (toggleButton2.isChecked()) {
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                } else {
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                }
            }
        });
        /*naverMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                makeMarker(mNaverMap,latLng);
            }
        });*/

        naverMap.addOnCameraChangeListener(new NaverMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(int i, boolean b) {
            }
        });

        naverMap.addOnLocationChangeListener(new NaverMap.OnLocationChangeListener() {
            @Override
            public void onLocationChange(@NonNull Location location) {
                showMessage(location.getLatitude() + ", " + location.getLongitude());
            }
        });

        naverMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                State vehicleState = drone.getAttribute(AttributeType.STATE);
                VehicleMode vehicleMode = vehicleState.getVehicleMode();

                point = new LatLong(latLng.latitude,latLng.longitude);
                marker.setPosition(latLng);
                marker.setIcon(MarkerIcons.BLACK);
                marker.setIconTintColor(Color.YELLOW);
                marker.setMap(mNaverMap);

                if(vehicleMode.equals(VehicleMode.COPTER_GUIDED)){
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                        new AbstractCommandListener() {
                            @Override

                            public void onSuccess() {

                                ControlApi.getApi(drone).goTo(point, true, null);
                            }

                            @Override

                            public void onError(int i) {

                            }
                            @Override
                            public void onTimeout() {
                            }
                        });
                }
                else {
                    Intent intent = new Intent(MainActivity.this, EventActivity.class);
                    intent.putExtra("data", "확인하시면 가이드모드로 전환 후 기체가 이동됩니다.");
                    startActivityForResult(intent, 3);
                }

            }

        });

        initMap();
    }
    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // DroneKit-Android Listener
    // ==========================================================

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    // Drone Listener
    // ==========================================================

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYAW();
                break;

            case AttributeEvent.GPS_COUNT:
                updateSatellite();
                break;

            case AttributeEvent.GPS_POSITION:
                updateMap();
                leadline();
                break;

            /*case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;*/

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    // UI Events
    // ==========================================================

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            this.drone.connect(ConnectionParameter.newUdpConnection(null));
        }

    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    public static boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Intent intent = new Intent(this, EventActivity.class);
        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {

                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            intent.putExtra("data", "지정한 이륙고도까지 기체가 상승합니다\n안전거리를 유지하세요");
            startActivityForResult(intent, 2);

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            intent.putExtra("data", "모터를 가동합니다.\n모터가 고속으로 회전합니다.");
            startActivityForResult(intent, 1);
        }
    }


    public void onAltitudeTap(View view) {
        Button plusButton = (Button) findViewById(R.id.plusButton);
        Button minusButton = (Button) findViewById(R.id.minusButton);

        if(plusButton.getVisibility() == View.INVISIBLE)
        {
            plusButton.setVisibility(View.VISIBLE);
            minusButton.setVisibility(View.VISIBLE);
        }
        else {
            plusButton.setVisibility(View.INVISIBLE);
            minusButton.setVisibility(View.INVISIBLE);
        }
    }

    public void onMapTap(View view) {
        Button lockButton = (Button) findViewById(R.id.lockButton);
        Button moveButton = (Button) findViewById(R.id.moveButton);

        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapButton = (Button) findViewById(R.id.mapButton);
                mapButton.setText("맵 잠금");
            }
        });
        moveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button mapButton = (Button) findViewById(R.id.mapButton);
                mapButton.setText("맵 이동");
            }
        });
        if(lockButton.getVisibility() == View.INVISIBLE)
        {
            lockButton.setVisibility(View.VISIBLE);
            moveButton.setVisibility(View.VISIBLE);
        }
        else {
            lockButton.setVisibility(View.INVISIBLE);
            moveButton.setVisibility(View.INVISIBLE);
        }
    }

    public void onPlusTap(View view) {
        Button altitudeButton = (Button) findViewById(R.id.altitudeButton);
        if(takeoffAltitude < 10){
            takeoffAltitude += 0.5;
            altitudeButton.setText(Double.toString(takeoffAltitude)+"m\n"+"이륙고도");
        }
    }
    public void onMinusTap(View view) {
        Button altitudeButton = (Button) findViewById(R.id.altitudeButton);
        if(takeoffAltitude > 0) {
            takeoffAltitude -= 0.5;
            altitudeButton.setText(Double.toString(takeoffAltitude)+"m\n"+"이륙고도");
        }

    }


    // UI updating
    // ==========================================================

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnARM);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(requestCode==1){
                if(resultCode==RESULT_OK){
                    //데이터 받기
                    VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {

                        @Override
                        public void onError(int executionError) {
                            alertUser("Unable to arm vehicle.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Arming operation timed out.");
                        }
                    });
                }
                else if(resultCode==RESULT_CANCELED) {
                    // Connect
                    alertUser("restart armButton");
                }
            }
            else if(requestCode==2){
                if(resultCode==RESULT_OK){
                    //데이터 받기
                    ControlApi.getApi(this.drone).takeoff(takeoffAltitude, new AbstractCommandListener() {

                        @Override
                        public void onSuccess() {
                            alertUser("Taking off...");
                        }

                        @Override
                        public void onError(int i) {
                            alertUser("Unable to take off.");
                        }

                        @Override
                        public void onTimeout() {
                            alertUser("Unable to take off.");
                        }
                    });
                }
                else if(resultCode==RESULT_CANCELED) {
                    alertUser("restart takeoffButton");
                }
        }
            else if(requestCode==3) {
                if (resultCode == RESULT_OK) {
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                            new AbstractCommandListener() {
                                @Override

                                public void onSuccess() {

                                    ControlApi.getApi(drone).goTo(point, true, null);
                                }

                                @Override

                                public void onError(int i) {

                                }
                                @Override
                                public void onTimeout() {
                                }
                            });
                }
                else if(resultCode==RESULT_CANCELED) {
                    alertUser("restart GuidedMode");
                }
            }
    }

    protected void updateBattery() {
        TextView batteryTextView = (TextView) findViewById(R.id.voltageValueTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        batteryTextView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }
    protected void updateYAW(){
        TextView droneYAWTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude droneAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);

        droneYAWTextView.setText(String.format("%3.1f", droneAttitude.getYaw()) + "deg");
        //droneYAWTextView.setText(String.format(Double.toString(droneAttitude.getYaw())));
    }

    protected void updateSatellite(){
        TextView droneSatellite = (TextView) findViewById(R.id.satelliteValueTextView);
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        //droneSatellite.setText(String.format("위성: %d", 14));
        droneSatellite.setText(String.format("%d", droneGPS.getSatellitesCount()));
    }

    protected void updateMap(){
        LatLong currentLatlongLocation = getCurrentLocation();
        LatLng currentLatlngLocation = new LatLng(currentLatlongLocation.getLatitude(),currentLatlongLocation.getLongitude());
        Log.d("lng","위도 : "+ currentLatlongLocation.getLatitude() + "경도 : "+ currentLatlongLocation.getLongitude());
        //droneMovePath.add(currentLatlngLocation);
        Attitude droneAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);

        locationOverlay = mNaverMap.getLocationOverlay();
        locationOverlay.setVisible(true);
        locationOverlay.setPosition(currentLatlngLocation);
        locationOverlay.setBearing((float) droneAttitude.getYaw() - 90);
        locationOverlay.setIcon(OverlayImage.fromResource(R.drawable.drone));

        coords.add(currentLatlngLocation);
        polyline.setCoords(coords);

        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatlngLocation);
        mNaverMap.moveCamera(cameraUpdate);

        if(CheckGoal(drone,currentLatlngLocation)){
            marker.setMap(null);
        }
    }

    protected LatLong getCurrentLocation(){
        Gps gps = this.drone.getAttribute(AttributeType.GPS);
        return gps.getPosition();
    }

    /*=======================================================================
    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }*/

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    // Helper methods
    // ==========================================================


    protected LatLng getCurrentLatLng(){
        LatLng currentLatlngLocation = new LatLng(0,0);

        try {
            LatLong currentLatlongLocation = getCurrentLocation();
            currentLatlngLocation = new LatLng(currentLatlongLocation.getLatitude(),currentLatlongLocation.getLongitude());

        }
        catch(NullPointerException e) {
            showMessage("GPS 수신이 불안정 합니다.");
        }

        return currentLatlngLocation;
    }


    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        recycler_list.add("★ "+ message);
        refreshRecyclerView();
        Log.d(TAG, message);
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void initMap(){
        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setCompassEnabled(false);
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setZoomControlEnabled(false);

        mNaverMap.setCameraPosition(new CameraPosition(DEFAULT_LATLNG, DEFAULT_ZOOM_LEVEL));
        mNaverMap.setMapType((NaverMap.MapType.Hybrid));
    }

    private void makeMarker(@NonNull NaverMap naverMap, @NonNull LatLng latLng){
        Marker marker = new Marker();
        marker.setPosition(latLng);
        marker.setMap(naverMap);
    }

    protected void leadline() {
        LatLong currentLatlongLocation = getCurrentLocation();
        LatLng currentLatlngLocation = new LatLng(currentLatlongLocation.getLatitude(),currentLatlongLocation.getLongitude());
        Attitude droneAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        if (currentLatlngLocation != null) {
            double dx = currentLatlongLocation.getLatitude() + 0.0005*Math.cos(Math.toRadians(droneAttitude.getYaw()));
            double dy = currentLatlongLocation.getLongitude() + 0.0005*Math.sin(Math.toRadians(droneAttitude.getYaw()));

            polyleadline.setCoords(Arrays.asList(
                    new LatLng(currentLatlongLocation.getLatitude(),currentLatlongLocation.getLongitude()),
                    new LatLng(dx,dy)
            ));
            polyleadline.setColor(Color.YELLOW);
            polyleadline.setPattern(10, 5);
            polyleadline.setMap(mNaverMap);
            Log.d("leadline","leadline");
        }
    }

    private void takePhoto() {
        SoloCameraApi.getApi(drone).takePhoto(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Photo taken.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while trying to take the photo: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timeout while trying to take the photo.");
            }
        });
    }

    private void toggleVideoRecording() {
        SoloCameraApi.getApi(drone).toggleVideoRecording(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Video recording toggled.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while trying to toggle video recording: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timeout while trying to toggle video recording.");
            }
        });
    }

    private void startVideoStream(Surface videoSurface) {
        SoloCameraApi.getApi(drone).startVideoStream(videoSurface, videoTag, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Successfully started the video stream. ");

                if (stopVideoStream != null)
                    stopVideoStream.setEnabled(true);

                if (startVideoStream != null)
                    startVideoStream.setEnabled(false);

                if (startVideoStreamUsingObserver != null)
                    startVideoStreamUsingObserver.setEnabled(false);

                if (stopVideoStreamUsingObserver != null)
                    stopVideoStreamUsingObserver.setEnabled(false);
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while starting the video stream: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timed out while attempting to start the video stream.");
            }
        });
    }

    DecoderListener decoderListener = new DecoderListener() {
        @Override
        public void onDecodingStarted() {
            alertUser("MediaCodecManager: video decoding started...");
        }

        @Override
        public void onDecodingError() {
            alertUser("MediaCodecManager: video decoding error...");
        }

        @Override
        public void onDecodingEnded() {
            alertUser("MediaCodecManager: video decoding ended...");
        }
    };


    private void startVideoStreamForObserver() {
        getApi(drone).startVideoStream(videoTag, new ExperimentalApi.IVideoStreamCallback() {
            @Override
            public void onVideoStreamConnecting() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully obtained lock for drone video stream.");
                    }
                });
            }

            @Override
            public void onVideoStreamConnected() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully opened drone video connection.");

                        if (stopVideoStreamUsingObserver != null)
                            stopVideoStreamUsingObserver.setEnabled(true);

                        if (startVideoStreamUsingObserver != null)
                            startVideoStreamUsingObserver.setEnabled(false);

                        if (stopVideoStream != null)
                            stopVideoStream.setEnabled(false);

                        if (startVideoStream != null)
                            startVideoStream.setEnabled(false);
                    }
                });

                mediaCodecManager.stopDecoding(new DecoderListener() {
                    @Override
                    public void onDecodingStarted() {
                    }

                    @Override
                    public void onDecodingError() {
                    }

                    @Override
                    public void onDecodingEnded() {
                        try {
                            mediaCodecManager.startDecoding(new Surface(videoView.getSurfaceTexture()),
                                    decoderListener);
                        } catch (IOException | IllegalStateException e) {
                            Log.e(TAG, "Unable to create media codec.", e);
                            if (decoderListener != null)
                                decoderListener.onDecodingError();
                        }
                    }
                });
            }

            @Override
            public void onVideoStreamDisconnecting() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully released lock for drone video stream.");
                    }
                });
            }

            @Override
            public void onVideoStreamDisconnected() {
                runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        alertUser("Successfully closed drone video connection.");

                        if (stopVideoStreamUsingObserver != null)
                            stopVideoStreamUsingObserver.setEnabled(false);

                        if (startVideoStreamUsingObserver != null)
                            startVideoStreamUsingObserver.setEnabled(true);

                        if (stopVideoStream != null)
                            stopVideoStream.setEnabled(false);

                        if (startVideoStream != null)
                            startVideoStream.setEnabled(true);
                    }
                });

                mediaCodecManager.stopDecoding(decoderListener);
            }

            @Override
            public void onError(int executionError) {
                alertUser("Error while getting lock to vehicle video stream: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Timed out while attempting to get lock for vehicle video stream.");
            }

            @Override
            public void onAsyncVideoStreamPacketReceived(byte[] data, int dataSize) {
                mediaCodecManager.onInputDataReceived(data, dataSize);
            }
        });
    }

    private void stopVideoStream() {
        SoloCameraApi.getApi(drone).stopVideoStream(videoTag, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                if (stopVideoStream != null)
                    stopVideoStream.setEnabled(false);

                if (startVideoStream != null)
                    startVideoStream.setEnabled(true);

                if (stopVideoStreamUsingObserver != null)
                    stopVideoStreamUsingObserver.setEnabled(false);

                if (startVideoStreamUsingObserver != null)
                    startVideoStreamUsingObserver.setEnabled(true);
            }

            @Override
            public void onError(int executionError) {
            }

            @Override
            public void onTimeout() {
            }
        });
    }

    private void stopVideoStreamForObserver() {
        getApi(drone).stopVideoStream(videoTag);
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }


    public void btnclearRecycler(View view) {
        clearRecyclerMessage();
    }

    /*public void test_btn(View view) {
        sendRecyclerMessage(String.format("%d 굉장히엄청나게대단하게심각하게긴텍스트",testCount++));
    }

    private void sendRecyclerMessage(String message) {
        recycler_list.add(String.format("★~" + message));
        refreshRecyclerView();
    }*/

    public void clearRecyclerMessage(){
        recycler_list.clear();
        refreshRecyclerView();
    }

    private void refreshRecyclerView() {
        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 리사이클러뷰에 SimpleAdapter 객체 지정.
        SimpleTextAdapter adapter = new SimpleTextAdapter(recycler_list);
        recyclerView.setAdapter(adapter);

        recyclerView.scrollToPosition(recycler_list.size()-1);
        //recyclerView.smoothScrollToPosition(recycler_list.size()-1);
    }

}