package com.example.currentplacedetailsonmap;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ntt.customgaugeview.library.GaugeView;

import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Subscriber;
import com.opentok.android.OpentokError;
import android.Manifest;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import android.opengl.GLSurfaceView;
import android.widget.ToggleButton;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivityCurrentPlace extends AppCompatActivity implements OnMapReadyCallback,Session.SessionListener,
        PublisherKit.PublisherListener, LocationListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, GoogleMap.OnCameraMoveStartedListener {

    private static final String TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;
    private GaugeView gv_meter;

    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private static final int M_MAX_ENTRIES = 5;
    private String[] mLikelyPlaceNames;
    private String[] mLikelyPlaceAddresses;
    private String[] mLikelyPlaceAttributions;
    private LatLng[] mLikelyPlaceLatLngs;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 1 * 1000;  /* 1 sec */
    private long FASTEST_INTERVAL = 1000; /* 1 sec */
    private boolean flag = false;
    private boolean recenterFlag = true;

    private TextView batLvl,batTemp,carTemp,rpm,speed,voltage,current;
    private Button info,video;
    private ImageView info1,tick_fire,tick_parking,tick_tele,fire,tele,parking;
    private int num=0,num1=0;

    private static String API_KEY = "46515762";
    private static String SESSION_ID = "1_MX40NjQzMTk5Mn5-MTU3MDAyNzY4NjY0MX5BdDV2cjd2eDVYc0o2YUpOZlBncUhtbjB-fg";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjUxNTc2MiZzaWc9NDczNTE2NjMwOWQ3NTkxNzdjOWU2NWFhYjc4ZDM4Y2MwZWYyMjVmMDpzZXNzaW9uX2lkPTJfTVg0ME5qVXhOVGMyTW41LU1UVTRNakExTVRBNE9UYzRPWDVNU2pKWlRtSnlSU3QzZUVGSmNsbHBPVGx5UmtSTFJEWi1mZyZjcmVhdGVfdGltZT0xNTgyMDUxMTEyJm5vbmNlPTAuODAxMTA3NjA1ODk5MTYxMSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTg0NjM5NTExJmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String LOG_TAG = MapsActivityCurrentPlace.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    private Session mSession;
    private Publisher mPublisher;
    private Subscriber mSubscriber;
    private FrameLayout mPublisherViewContainer;
    private FrameLayout mSubscriberViewContainer;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    Dialog dialog,dialog1,alert_fire,alert_parking,telemetry;
    private Button dismiss,troubleshoot;
    private ArrayList<LatLng> points; //added
    Polyline line; //added
    private ToggleButton sw,lap;
    private Button recenter;
    private float zoom = 18.0f;
    private float tilt= 40.0f;
    private double latitude,longitude;
    private Button tap;
    private int count=0;
    private boolean flagLap = false;
    private double startingLat,startingLong;
    private int lapCount = 0;
    private boolean outZone = false;
    private TextView lapC,bar;
    private long startTime,endTime;
    private TextView timeLap;
    private TextView timer,timerHeading;
    private int h=0,m=0,s=0;
    private float distance=0;
    private double pLat,pLong;
    private int oncecalled=0;
    private TextView distDisp;
    private TextView gear1,gear2;
    private Button reset;
    private int resCount=0,rowNum=1,spd=0;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("Data");
    SharedPreferences sp;
    SharedPreferences.Editor edit;

    Workbook wb = new HSSFWorkbook();
    Cell cell = null;
    Sheet sheet = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_maps);
        gv_meter =findViewById(R.id.gv_meter);
        gv_meter.setShowRangeValues(true);
        gv_meter.setTargetValue(0);
        batLvl = findViewById(R.id.batLvl);
        batTemp = findViewById(R.id.batTemp);
        carTemp = findViewById(R.id.carTemp);
        rpm = findViewById(R.id.value);
        info = findViewById(R.id.info1);
        video = findViewById(R.id.graph1);
        speed = findViewById(R.id.speed1);
        voltage = findViewById(R.id.bat_voltage);
        current = findViewById(R.id.bat_current);
        info1 = findViewById(R.id.info);

        dialog = new Dialog(MapsActivityCurrentPlace.this);
        dialog.setContentView(R.layout.info_layout);
        tick_fire = dialog.findViewById(R.id.tick_fire);
        tick_parking = dialog.findViewById(R.id.tick_parking);
        tick_tele = dialog.findViewById(R.id.tick_tele);
        fire = dialog.findViewById(R.id.fire);
        parking = dialog.findViewById(R.id.parking);
        tele = dialog.findViewById(R.id.tele);
        dismiss = dialog.findViewById(R.id.dismiss);
        points = new ArrayList<LatLng>();
        sw = findViewById(R.id.path);
        recenter = findViewById(R.id.recenter);
        tap = findViewById(R.id.tap);
        lap = findViewById(R.id.lapCounter);
        lapC = findViewById(R.id.lapDisplay);
        bar = findViewById(R.id.bar);
        timeLap = findViewById(R.id.time);
        timer = findViewById(R.id.timer);
        timerHeading = findViewById(R.id.timeHeading);
        sp = getSharedPreferences("distance",MODE_PRIVATE);
        distance = sp.getFloat("distance",0);
        distDisp = dialog.findViewById(R.id.distance);
        reset = dialog.findViewById(R.id.reset);
        distDisp.setText(String.format("%.2f\nkms", distance));
        gear1 = findViewById(R.id.gear1);
        gear2 = findViewById(R.id.gear2);

        sheet = wb.createSheet("Data");
        Row r = sheet.createRow(0);
        cell = r.createCell(0);
        cell.setCellValue("Time");
        cell = r.createCell(1);
        cell.setCellValue("Bat Lvl");
        cell = r.createCell(2);
        cell.setCellValue("Speed");
        cell = r.createCell(3);
        cell.setCellValue("Bat Temp");
        cell = r.createCell(4);
        cell.setCellValue("Car Temp");
        cell = r.createCell(5);
        cell.setCellValue("RPM");
        cell = r.createCell(6);
        cell.setCellValue("Voltage");
        cell = r.createCell(7);
        cell.setCellValue("Current");
        cell = r.createCell(8);
        cell.setCellValue("Smoke");
        cell = r.createCell(9);
        cell.setCellValue("Parking");
        cell = r.createCell(10);
        cell.setCellValue("Motor");
        cell = r.createCell(11);
        cell.setCellValue("Gear");
        sheet.setColumnWidth(0,(10*560));
        sheet.setColumnWidth(1,(10*200));
        sheet.setColumnWidth(2,(10*200));
        sheet.setColumnWidth(3,(10*200));
        sheet.setColumnWidth(4,(10*200));
        sheet.setColumnWidth(5,(10*200));
        sheet.setColumnWidth(6,(10*200));
        sheet.setColumnWidth(7,(10*200));
        sheet.setColumnWidth(8,(10*200));
        sheet.setColumnWidth(9,(10*200));
        sheet.setColumnWidth(10,(10*200));
        sheet.setColumnWidth(11,(10*200));

        File file = new File(getExternalFilesDir(null),"esr.xls");
        FileOutputStream outputStream =null;

        try {
            outputStream=new FileOutputStream(file);
            wb.write(outputStream);
        } catch (java.io.IOException e) {
            e.printStackTrace();

            try {
                outputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        CountDownTimer timerCountDown = new CountDownTimer(36000000,1000) {
            @Override
            public void onTick(long l) {
                if(s<59){
                    s++;
                }
                else{
                    s=0;
                    if(m<59){
                        m++;
                    }
                    else {
                        h++;
                        m=0;
                    }
                }
                timer.setText(String.format("%dh %dm %ds",h,m,s));
            }
            @Override
            public void onFinish() {
            }
        };

        lap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    lap.setBackgroundResource(R.drawable.lapon);
                    flagLap = true;
                    outZone = false;
                    startingLat = latitude;
                    startingLong = longitude;
                    Toast t = Toast.makeText(MapsActivityCurrentPlace.this,"Lap Counter Activated", Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.CENTER,400,250);
                    t.show();
                    startTime = SystemClock.elapsedRealtime();
                    lapC.setText(Integer.toString(lapCount));
                    lapC.setBackgroundColor(Color.argb(200,255,255,255));
                    lapC.setTextColor(Color.argb(255,0,0,0));
                    bar.setBackgroundColor(Color.argb(255,0,0,0));
                    bar.setTextColor(Color.argb(255,255,255,255));
                    timer.setText("0h 0m 0s");
                    timer.setBackgroundColor(Color.argb(200,255,255,255));
                    timer.setTextColor(Color.argb(255,0,0,0));
                    timerHeading.setBackgroundColor(Color.argb(255,0,0,0));
                    timerHeading.setTextColor(Color.argb(255,255,255,255));
                    timerCountDown.start();
                }
                else{
                    lap.setBackgroundResource(R.drawable.lap);
                    flagLap = false;
                    lapCount = 0;
                    myRef.child("Laps").setValue(lapCount);
                    myRef.child("Lap Min").setValue("0");
                    myRef.child("Lap Sec").setValue(0);
                    Toast t = Toast.makeText(MapsActivityCurrentPlace.this,"Lap Counter De-Activated", Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.CENTER,400,250);
                    t.show();
                    lapC.setBackgroundColor(Color.argb(0,255,255,255));
                    lapC.setTextColor(Color.argb(0,0,0,0));
                    bar.setBackgroundColor(Color.argb(0,0,0,0));
                    bar.setTextColor(Color.argb(0,255,255,255));
                    timerHeading.setBackgroundColor(Color.argb(0,255,255,255));
                    timerHeading.setTextColor(Color.argb(0,0,0,0));
                    timer.setBackgroundColor(Color.argb(0,0,0,0));
                    timer.setTextColor(Color.argb(0,255,255,255));
                    h=m=s=0;
                    timerCountDown.cancel();
                }
            }
        });

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    sw.setBackgroundResource(R.drawable.trail_on);
                    flag=true;
                    Toast t = Toast.makeText(MapsActivityCurrentPlace.this,"Path Tracking Activated", Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.CENTER,400,250);
                    t.show();
                    points.add(new LatLng(latitude,longitude));
                }
                else{
                    Toast t = Toast.makeText(MapsActivityCurrentPlace.this,"Path Tracking De-Activated", Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.CENTER,400,250);
                    t.show();
                    sw.setBackgroundResource(R.drawable.trail);
                    flag = false;
                    mMap.clear();
                    points.clear();
                    oncecalled=0;
                }
            }
        });

        tap.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                count++;
                if(count==5){
                    count=0;
                    Toast t = Toast.makeText(MapsActivityCurrentPlace.this,"Made by A&A", Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.CENTER,0,0);
                    t.show();
                }
            }
        });

        // add button listener
        recenter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                recenterFlag = true;
                mMap.moveCamera(CameraUpdateFactory.newLatLng(
                        new LatLng(latitude,
                                longitude)));
            }
        });

        // add button listener
        info.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // custom dialog
                Window win = dialog.getWindow();
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(200,255,255,255)));
                WindowManager.LayoutParams wlp = win.getAttributes();
                win.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT);
                wlp.x=1000;
                wlp.y=90;
                win.setAttributes(wlp);
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.show();
                dismiss.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        dialog.dismiss();
                    }
                });
                reset.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        resCount++;
                        if(resCount==2) {
                            edit = sp.edit();
                            edit.putFloat("distance", 0);
                            edit.commit();
                            distance = 0;
                            myRef.child("Total_Distance_Travelled").setValue(distance);
                            distDisp.setText("0.00\nkms");
                            resCount=0;
                        }
                    }
                });
            }
        });

        alert_fire = new Dialog(MapsActivityCurrentPlace.this);
        alert_fire.setContentView(R.layout.alert_fire);
        alert_fire.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(30,255,0,0)));

        alert_parking = new Dialog(MapsActivityCurrentPlace.this);
        alert_parking.setContentView(R.layout.alert_parking);
        alert_parking.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(30,255,0,0)));

        telemetry = new Dialog(MapsActivityCurrentPlace.this);
        telemetry.setContentView(R.layout.telemetry);
        telemetry.setCancelable(false);
        telemetry.setCanceledOnTouchOutside(false);
        troubleshoot = telemetry.findViewById(R.id.troubleshoot);
        telemetry.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(255,255,226,226)));

        dialog1 = new Dialog(MapsActivityCurrentPlace.this);
        dialog1.setContentView(R.layout.videocall);

        // add button listener
        video.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // custom dialog
                requestPermissions();
                Window win = dialog1.getWindow();
                WindowManager.LayoutParams wlp = win.getAttributes();
                win.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT);
                wlp.x=1000;
                wlp.y=90;
                win.setAttributes(wlp);
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog1.show();

            }
        });

        bluetooth();

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void lapCount(){
        double latDiff = (latitude > startingLat) ? (latitude - startingLat) : (startingLat - latitude);
        double longDiff = (longitude > startingLong) ? (longitude - startingLong) : (startingLong - longitude);
        if(!outZone) {
            if ((latDiff >= 0.00015) || (longDiff >= 0.00015)) {// out of zone
                outZone = true;
            }
        }
        else{
            if ((latDiff < 0.00007) && (longDiff < 0.00007)) {// inside  zone
                outZone = false;
                lapCount++;
                myRef.child("Laps").setValue(lapCount);
                endTime = SystemClock.elapsedRealtime();
                double diff = (endTime-startTime)/60000.0;
                String str = Double.toString(diff);
                String[] s = str.split("\\.");
                String q = ""+s[1].charAt(0)+s[1].charAt(1);
                int sec = (int)(Integer.parseInt(q)*0.6);
                myRef.child("Lap Min").setValue(s[0]);
                myRef.child("Lap Sec").setValue(sec);
                lapC.setText(Integer.toString(lapCount));
                startTime = SystemClock.elapsedRealtime();
                timeLap.setText(s[0]+" min  "+String.format("%d sec",sec));
                countDown();
            }
        }
    }

    public void countDown(){
        CountDownTimer timer = new CountDownTimer(5000,1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                timeLap.setText("");
            }
        }.start();
    }

    @Override
    public void onCameraMoveStarted(int reasonCode) {
        if (reasonCode == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            recenterFlag = false;
        }
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions() {
        String[] perms = {Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // initialize view objects from your layout
            mPublisherViewContainer = dialog1.findViewById(R.id.publisher_container);
            mSubscriberViewContainer = dialog1.findViewById(R.id.subscriber_container);

            // initialize and connect to the session
            mSession = new Session.Builder(this, API_KEY, SESSION_ID).build();
            mSession.setSessionListener(this);
            mSession.connect(TOKEN);

        }
        else {
        EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, perms);
        }
    }

    // SessionListener methods

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");

        mPublisher = new Publisher.Builder(this).build();
        mPublisher.setPublisherListener(this);

        mPublisherViewContainer.addView(mPublisher.getView());

        if (mPublisher.getView() instanceof GLSurfaceView) {
        ((GLSurfaceView) mPublisher.getView()).setZOrderOnTop(true);
        }

        mSession.publish(mPublisher);
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");

        if (mSubscriber == null) {
        mSubscriber = new Subscriber.Builder(this, stream).build();
        mSession.subscribe(mSubscriber);
        mSubscriberViewContainer.addView(mSubscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");

        if (mSubscriber != null) {
        mSubscriber = null;
        mSubscriberViewContainer.removeAllViews();
        }
    }


    @Override
    public void onError(Session session, OpentokError opentokError) {
        Log.e(LOG_TAG, "Session error: " + opentokError.getMessage());
    }

    // PublisherListener methods

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamCreated");
    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
        Log.i(LOG_TAG, "Publisher onStreamDestroyed");
    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {
        Log.e(LOG_TAG, "Publisher error: " + opentokError.getMessage());
    }

    void bluetooth(){
        try {
            info1.setImageResource(R.drawable.extras);
            tele.setImageResource(R.drawable.telemetry);
            tick_tele.setImageResource(R.drawable.tick);
            findBT();
            openBT();
        } catch (IOException ex) {
            /*
            telemetry.show();
            info1.setImageResource(R.drawable.extras);
            tele.setImageResource(R.drawable.telemetry);
            tick_tele.setImageResource(R.drawable.cross);
            troubleshoot.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    telemetry.dismiss();
                    try {
                        closeBT();
                    } catch (Exception ex) {
                    }
                }
            });
            */
        }
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast msg = Toast.makeText(getApplicationContext(),"No Bluetooth Module Found", Toast.LENGTH_SHORT);
            msg.show();
            //label.setText("No bluetooth adapter available");
        }
        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Toast msg = Toast.makeText(getApplicationContext(),"Searching Bluetooth Module", Toast.LENGTH_SHORT);
        msg.show();
       // label.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();
        sendData('A');
        Toast msg = Toast.makeText(getApplicationContext(),"Bluetooth Module Connected", Toast.LENGTH_SHORT);
        msg.show();
        //label.setText("Bluetooth Opened");
    }
    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 120; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Row row = sheet.createRow(rowNum++);
                                            cell = row.createCell(0);
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                                            String time = sdf.format(new Date());
                                            cell.setCellValue(time);
                                            Log.e("DATA",data);
                                            String[] s=data.trim().split(",");
                                            batLvl.setText(s[0]+"%");
                                            //gv_meter.setTargetValue(Integer.valueOf(s[1]));
                                            speed.setText(s[9]);//motor rpm
                                            batTemp.setText(s[2]);
                                            carTemp.setText(s[3]);
                                            rpm.setText(s[4]);
                                            voltage.setText(s[5]+" V");
                                            current.setText(s[6]+" A");
                                            if(s[7].equals("1")){
                                                num++;
                                                if (num==1) alert_fire.show();
                                                info1.setImageResource(R.drawable.extras);
                                                fire.setImageResource(R.drawable.fire);
                                                tick_fire.setImageResource(R.drawable.cross);
                                            }
                                            else{
                                                num=0;
                                                info1.setImageResource(R.drawable.extras);
                                                fire.setImageResource(R.drawable.fire);
                                                tick_fire.setImageResource(R.drawable.tick);
                                                alert_fire.dismiss();
                                            }
                                            if(s[8].equals("1")){
                                                num1++;
                                                if (num1==1) alert_parking.show();
                                                info1.setImageResource(R.drawable.extras);
                                                parking.setImageResource(R.drawable.parking);
                                                tick_parking.setImageResource(R.drawable.cross);
                                            }
                                            else{
                                                num1=0;
                                                info1.setImageResource(R.drawable.extras);
                                                parking.setImageResource(R.drawable.parking);
                                                tick_parking.setImageResource(R.drawable.tick);
                                                alert_parking.dismiss();
                                            }
                                            if(s[10].equals("1")){
                                                gear1.setTextColor(Color.BLACK);
                                                gear2.setTextColor(Color.GRAY);
                                                gear1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                                                gear2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) gear1.getLayoutParams();
                                                params.horizontalBias = 0.1f; // here is one modification for example. modify anything else you want :)
                                                gear1.setLayoutParams(params); // request the view to use the new modified params
                                                ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) gear2.getLayoutParams();
                                                params1.horizontalBias = 0.3f; // here is one modification for example. modify anything else you want :)
                                                gear2.setLayoutParams(params1); // request the view to use the new modified params
                                            }
                                            else{
                                                gear2.setTextColor(Color.BLACK);
                                                gear1.setTextColor(Color.GRAY);
                                                gear2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                                                gear1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) gear2.getLayoutParams();
                                                params.horizontalBias = 0.1f; // here is one modification for example. modify anything else you want :)
                                                gear2.setLayoutParams(params); // request the view to use the new modified params
                                                ConstraintLayout.LayoutParams params1 = (ConstraintLayout.LayoutParams) gear1.getLayoutParams();
                                                params1.horizontalBias = 0.3f; // here is one modification for example. modify anything else you want :)
                                                gear1.setLayoutParams(params1); // request the view to use the new modified params
                                            }
                                            myRef.child("Battery Level").setValue(s[0]);
                                            myRef.child("Speed").setValue(s[1]);
                                            myRef.child("Battery Temperature").setValue(s[2]);
                                            myRef.child("Car Temperature").setValue(s[3]);
                                            myRef.child("RPM").setValue(s[4]);
                                            myRef.child("Voltage").setValue(s[5]);
                                            myRef.child("Current").setValue(s[6]);
                                            myRef.child("Smoke Status").setValue(s[7]);
                                            myRef.child("Parking Status").setValue(s[8]);
                                            myRef.child("Motor").setValue(s[9]);
                                            myRef.child("Gear").setValue(s[10]);

                                            cell = row.createCell(1);
                                            cell.setCellValue(s[0]);
                                            cell = row.createCell(2);
                                            cell.setCellValue(spd);
                                            cell = row.createCell(3);
                                            cell.setCellValue(s[2]);
                                            cell = row.createCell(4);
                                            cell.setCellValue(s[3]);
                                            cell = row.createCell(5);
                                            cell.setCellValue(s[4]);
                                            cell = row.createCell(6);
                                            cell.setCellValue(s[5]);
                                            cell = row.createCell(7);
                                            cell.setCellValue(s[6]);
                                            cell = row.createCell(8);
                                            cell.setCellValue(s[7]);
                                            cell = row.createCell(9);
                                            cell.setCellValue(s[8]);
                                            cell = row.createCell(10);
                                            cell.setCellValue(s[9]);
                                            cell = row.createCell(11);
                                            cell.setCellValue(s[10]);

                                            File file = new File(getExternalFilesDir(null),"esr.xls");
                                            FileOutputStream outputStream =null;

                                            try {
                                                outputStream=new FileOutputStream(file);
                                                wb.write(outputStream);
                                            } catch (java.io.IOException e) {
                                                e.printStackTrace();
                                                try {
                                                    outputStream.close();
                                                } catch (IOException ex) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData(char c) throws IOException
    {
        String msg = Character.toString(c);
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
       // label.setText("Data Sent");
    }

    void closeBT()
    {
        stopWorker = true;
        //mmOutputStream.close();
        //mmInputStream.close();
        //mmSocket.close();
        Toast msg = Toast.makeText(getApplicationContext(),"Troubleshooting", Toast.LENGTH_SHORT);
        msg.show();
        bluetooth();
        //myLabel.setText("Bluetooth Closed");
    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout) findViewById(R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener(){
            @Override
            public void onCameraMoveStarted(int reasonCode) {
                if (reasonCode == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    recenterFlag = false;
                }
            }
        });
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setAllGesturesEnabled(true);
    }


    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            latitude = mLastKnownLocation.getLatitude();
                            longitude = mLastKnownLocation.getLongitude();
                            myRef.child("Latitude").setValue(latitude);
                            myRef.child("Longitude").setValue(longitude);
                            CameraPosition cm = new CameraPosition.Builder().target(new LatLng(latitude,longitude)).tilt(tilt).zoom(zoom).bearing(0).build();
                            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cm));
                            pLat = latitude;
                            pLong = longitude;
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, zoom));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        startLocationUpdates();

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {

            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        myRef.child("Latitude").setValue(latitude);
        myRef.child("Longitude").setValue(longitude);
        LatLng latLng = new LatLng(latitude, longitude); //you already have this
        //speed.setText(String.format("%d",(int)(location.getSpeed()*(18/5.0))));
        spd = (int)(location.getSpeed()*(18/5.0));
        gv_meter.setTargetValue((int)(location.getSpeed()*(18/5.0)));
        if(flagLap) lapCount();
        if(recenterFlag) mMap.moveCamera(CameraUpdateFactory.newLatLng(
                new LatLng(latitude,
                        longitude)));
        if(flag) {
            if(oncecalled<=30) {
                points.add(latLng);
                oncecalled++;
            }
            else{
                points.remove(0);
                points.add(latLng);
            }
            redrawLine(); //added
        }
        double latDiff = (latitude > pLat) ? (latitude - pLat) : (pLat - latitude);
        double longDiff = (longitude > pLong) ? (longitude - pLong) : (pLong - longitude);
        if ((latDiff >= 0.000179052) || (longDiff >=0.000179052)) {// out of zone
            distance+=0.02;
            pLat=latitude;
            pLong = longitude;
            edit = sp.edit();
            edit.putFloat("distance",distance);
            edit.commit();
            distDisp.setText(String.format("%.2f\nkms", distance));
            myRef.child("Total_Distance_Travelled").setValue(distance);
        }
    }

    @Override
    public void onStatusChanged(String s,int n, Bundle b){};

    @Override
    public void onProviderDisabled(String s){}

    @Override
    public void onProviderEnabled(String s){}

    private void redrawLine(){
        PolylineOptions options = new PolylineOptions().width(20).color(Color.BLUE).geodesic(true);
        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }
        line = mMap.addPolyline(options); //add Polyline
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "which" argument contains the position of the selected item.
                LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                String markerSnippet = mLikelyPlaceAddresses[which];
                if (mLikelyPlaceAttributions[which] != null) {
                    markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                }

                // Add a marker for the selected place, with an info window
                // showing information about that place.
                mMap.addMarker(new MarkerOptions()
                        .title(mLikelyPlaceNames[which])
                        .position(markerLatLng)
                        .snippet(markerSnippet));

                // Position the map's camera at the location of the marker.
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                        DEFAULT_ZOOM));
            }
        };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void train(View view) {
        Intent i = new Intent();
        i.setComponent(new ComponentName("cultoftheunicorn.marvel","cultoftheunicorn.marvel.NameActivity"));
        if(i != null) {
            startActivity(i);
            finish();
        }
    }

}
