package com.pinetree408.research.temporaltargeting;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    // sensors
    private Sensor mAccSensor;
    private Sensor mGyro;
    private Sensor mLinear;
    private Sensor mMagSensor;
    private SensorManager mSensorManager;

    private static final float SHAKE_THRESHOLD = 2.0f;
    private static final int SHAKE_WAIT_TIME_MS = 250;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private final float[] mXY = new float[2];

    private List<float[]> mSignal;
    int bufferSize = 10;

    // Alphabets
    String[] initAlpList = {
            "t", "a", "i", "s", "o",
            "w", "c", "b", "h", "m",
            //"p", "f", "d", "n", "r",
            //"l", "e", "g", "y", "u",
            //"k", "j", "v", "q", "x", "z"
    };
    float centerY = 135;

    ViewGroup alphabetContainer;

    List<String> alphabetList;

    TextView eventView;
    TextView addedEventView;

    private long mShakeTime = 0;

    private int modeFlag = 0;

    //Socket socket;

    String ip = "143.248.197.106";
    int port = 5000;

    Socket socket;

    String globalState = "MIDDLE";

    class TiltEvent {
        public String action;
        public float tiltValue;
        public TiltEvent( String action, float tiltValue) {
            this.action = action;
            this.tiltValue = tiltValue;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alphabetList = Arrays.asList(initAlpList);

        // init sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        mSignal = new ArrayList<>();

        // alpha
        alphabetContainer = (ViewGroup) findViewById(R.id.alphabet_view);
        generateAlpList();

        eventView = (TextView) findViewById(R.id.event);
        addedEventView = (TextView) findViewById(R.id.added_event);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            socket = IO.socket("http://" + ip + ":" + port + "/mynamespace");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                modeFlag = 1;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addedEventView.setText(Integer.toString(modeFlag));
                    }
                });
                Log.d("Socket", "connect");
            }

        }).on("response", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                try {
                    JSONObject response = (JSONObject) args[0];

                    String gesture = response.get("data").toString();


                    if (globalState.equals("MIDDLE")) {
                        if (gesture.equals("1")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    for (int i = 1; i < alphabetContainer.getChildCount(); i++) {
                                        TextView child = (TextView) alphabetContainer.getChildAt(i);
                                        if ((122.5 < child.getY()) && (child.getY() < 160)) {
                                            eventView.setText(child.getText());
                                            break;
                                        }
                                    }
                                }
                            });
                        } else if (gesture.equals("2")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    eventView.setText("");
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                Log.d("Socket", "disconnect");
            }

        });
    }

    public void generateAlpList() {
        for (int i = 0; i < alphabetList.size(); i++) {
            final TextView testView = new TextView(this);
            testView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50));
            testView.setY(i * 50 + centerY);
            testView.setText(alphabetList.get(i));
            testView.setText("Item " + i);
            testView.setGravity(Gravity.CENTER);
            testView.setTextSize(20);
            alphabetContainer.addView(testView);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (modeFlag == 1) {
            if (event.sensor.getType() != 2) {
                socket.emit("request",
                        Integer.toString(event.sensor.getType()),
                        (event.timestamp / 1000000),
                        Float.toString(event.values[0]),
                        Float.toString(event.values[1]),
                        Float.toString(event.values[2]));
            }
        }

        if(event.sensor.getType() ==  Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
            detectShake(event);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,  0, mMagnetometerReading.length);
        }

        TiltEvent te = updateOrientationAngles();
        globalState = te.action;

        View checkedView;
        boolean updateFlag = false;

        if (te.action.equals("UP")) {
            checkedView = alphabetContainer.getChildAt(alphabetContainer.getChildCount()-1);
            if ((checkedView.getY() - centerY) > 0) {
                updateFlag = true;
            }
        } else if (te.action.equals("DOWN")) {
            checkedView = alphabetContainer.getChildAt(1);
            if ((checkedView.getY() - centerY) < 0) {
                updateFlag = true;
            }
        }

        if (updateFlag) {
            for (int i = 1; i < alphabetContainer.getChildCount(); i++) {
                TextView child = (TextView) alphabetContainer.getChildAt(i);
                child.setY(child.getY() + (te.tiltValue * 1));
            }
        }
    }

    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX*gX + gY*gY + gZ*gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if(gForce > SHAKE_THRESHOLD) {
                if (modeFlag == 0) {
                    socket.connect();
                } else {
                    modeFlag = 0;
                    socket.disconnect();
                    addedEventView.setText(Integer.toString(modeFlag));
                }
            }
        }
    }

    public TiltEvent updateOrientationAngles() {
        mSensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        float yaw = mOrientationAngles[0];
        float pitch = mOrientationAngles[1];
        float roll = mOrientationAngles[2];
        convertToXY(mXY, pitch, roll, yaw);
        if (mSignal.size() < bufferSize) {
            mSignal.add(mXY);
        } else {
            mSignal.remove(0);
            mSignal.add(mXY);
        }

        float meanY = getYMean();

        String state = "";
        if (meanY < - 0.2) {
            state = "UP";
        } else if (meanY < 0.2) {
            state = "MIDDLE";
        } else {
            state = "DOWN";
        }

        return new TiltEvent(state, meanY);
    }

    public void convertToXY(float[] xy, float pitch, float roll, float yaw) {
        // Sun, Ke, et al. "Float: One-Handed and Touch-Free Target Selection on Smartwatches." Proceedings of the 2017 CHI Conference on Human Factors in Computing Systems. ACM, 2017.
        //pitch = -pitch;
        //roll = -roll;
        xy[0] = (float) (Math.cos(pitch) * Math.sin(roll));
        xy[1] = (float) (-1 * Math.sin(pitch));
    }

    public float getXMean() {
        float x = 0f;
        for (float[] item : mSignal) {
            x += item[0];
        }
        x /= mSignal.size();
        return x;
    }

    public float getYMean() {
        float y = 0f;
        for (float[] item : mSignal) {
            y += item[1];
        }
        y /= mSignal.size();
        return y;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mMagSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
