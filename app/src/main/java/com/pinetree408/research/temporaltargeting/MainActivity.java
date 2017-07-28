package com.pinetree408.research.temporaltargeting;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements SensorEventListener {

    // sensors
    private Sensor mAccSensor;
    private Sensor mMagSensor;
    private SensorManager mSensorManager;

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
            "p", "f", "d", "n", "r",
            "l", "e", "g", "y", "u",
            "k", "j", "v", "q", "x", "z"
    };
    float centerY = 135;

    ViewGroup alphabetContainer;


    TextView inputView;
    private List<TextView> suggestViewList;

    String nowInput;
    String preWords;

    LanguageModel lm;
    List<String> alphabetList;

    String[] mackenzieSet;
    TextView targetView;
    Random random;

    Long startTime;

    TextView tempView;

    private int dragThreshold = 30;
    private final double angleFactor = (double) 180/Math.PI;
    private float touchDownX, touchDownY;
    private long touchDownTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final InputStream inputStream = getResources().openRawResource(R.raw.word_set);
        InputStream[] params = {inputStream};
        new LmInitTask().execute(params);

        mackenzieSet = Mackenzie.mackenzieSet;
        targetView = (TextView) findViewById(R.id.target);
        random = new Random();
        targetView.setText(mackenzieSet[random.nextInt(mackenzieSet.length)]);

        alphabetList = Arrays.asList(initAlpList);

        // init sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSignal = new ArrayList<>();

        // alpha
        alphabetContainer = (ViewGroup) findViewById(R.id.alphabet_view);
        generateAlpList();

        inputView = (TextView) findViewById(R.id.input);
        suggestViewList = new ArrayList<>();
        suggestViewList.add((TextView) findViewById(R.id.suggest1));
        suggestViewList.add((TextView) findViewById(R.id.suggest2));
        suggestViewList.add((TextView) findViewById(R.id.suggest3));

        nowInput = "";
        preWords = "";

        tempView = (TextView) findViewById(R.id.temp);
        tempView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                int tempX = (int) event.getAxisValue(MotionEvent.AXIS_X);
                int tempY = (int) event.getAxisValue(MotionEvent.AXIS_Y);
                long eventTime = System.currentTimeMillis();

                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (tempView.getClass() == v.getClass()) {
                            touchDownTime = eventTime;
                            touchDownX = tempX;
                            touchDownY = tempY;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        long touchTime = eventTime - touchDownTime;
                        int xDir = (int) (touchDownX - tempX);
                        int yDir = (int) (touchDownY - tempY);
                        int len = (int) Math.sqrt(xDir * xDir + yDir * yDir);
                        int speed;
                        if (touchTime > 0) {
                            speed = (int) (len * 1000 / touchTime);
                        } else {
                            speed = 0;
                        }
                        if (len > dragThreshold) {
                            if (speed > 400) {
                                double angle = Math.acos((double) xDir / len) * angleFactor;
                                if (yDir < 0) {
                                    angle = 360 - angle;
                                }
                                angle += 45;
                                int id = (int) (angle / 90);
                                if (id > 3) {
                                    id = 0;
                                }
                                switch (id){
                                    case 0:
                                        //left
                                        if (nowInput.length() != 0) {
                                            nowInput = "";
                                        } else {
                                            String[] history = preWords.split("\\s");
                                            String newPreWords = "";
                                            for (int i = 0; i < history.length - 1; i++) {
                                                if (i != 0) {
                                                    newPreWords += " ";
                                                }
                                                newPreWords += history[i];
                                            }
                                            preWords = newPreWords;
                                        }
                                        inputView.setText(preWords);
                                        alphabetList = Arrays.asList(initAlpList);
                                        int size = alphabetContainer.getChildCount();
                                        for (int i = 1; i < size; i++) {
                                            alphabetContainer.removeViewAt(alphabetContainer.getChildCount()-1);
                                        }
                                        generateAlpList();
                                        for (int i = 0; i < suggestViewList.size(); i++){
                                            suggestViewList.get(i).setText("");
                                        }
                                        break;
                                    case 1:
                                        //top;
                                        break;
                                    case 2:
                                        //right
                                        break;
                                    case 3:
                                        //bottom;
                                        break;
                                }
                            }
                        } else {
                            if (startTime == null) {
                                startTime = System.currentTimeMillis();
                            }

                            List<Float> distanceList = new ArrayList<>();
                            for (int i = 1; i < alphabetContainer.getChildCount(); i++) {
                                View child = alphabetContainer.getChildAt(i);
                                float distance = Math.abs((alphabetContainer.getHeight() / 2.0f) - (child.getY() + 25));
                                distanceList.add(distance);
                            }
                            int minIndex = distanceList.indexOf(Collections.min(distanceList));
                            TextView resultView = (TextView) alphabetContainer.getChildAt(minIndex + 1);

                            nowInput = nowInput + resultView.getText().toString();
                            String[] params = {nowInput};
                            new WordSuggestionTask().execute(params);

                            alphabetList = lm.getAlphasFromPrefix(nowInput);
                            int size = alphabetContainer.getChildCount();
                            for (int i = 1; i < size; i++) {
                                alphabetContainer.removeViewAt(alphabetContainer.getChildCount()-1);
                            }
                            generateAlpList();

                            String visualizeString = "";
                            if (preWords.length() != 0) {
                                visualizeString = preWords + " " + nowInput;
                            } else {
                                visualizeString = nowInput;
                            }
                            inputView.setText(visualizeString);
                            break;
                        }
                        break;
                }
                return true;
            }
        });

        for (final TextView suggestView : suggestViewList) {
            suggestView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()){
                        case MotionEvent.ACTION_DOWN:
                            nowInput = "";
                            if (preWords.length() != 0) {
                                preWords = preWords + " " + suggestView.getText();
                            } else {
                                preWords = suggestView.getText().toString();
                            }
                            inputView.setText(preWords);
                            alphabetList = Arrays.asList(initAlpList);
                            int size = alphabetContainer.getChildCount();
                            for (int i = 1; i < size; i++) {
                                alphabetContainer.removeViewAt(alphabetContainer.getChildCount()-1);
                            }
                            generateAlpList();
                            for (int i = 0; i < suggestViewList.size(); i++){
                                suggestViewList.get(i).setText("");
                            }

                            if (preWords.equals(targetView.getText())) {
                                targetView.setText(mackenzieSet[random.nextInt(mackenzieSet.length)]);
                                nowInput = "";
                                preWords = "";
                                inputView.setText("");
                                double wpm = 5.0 / (((System.currentTimeMillis() - startTime) / 1000.0) / 60);
                                tempView.setText(Double.toString(wpm));
                                startTime = System.currentTimeMillis();
                            }

                            break;
                    }
                    return false;
                }
            });
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void generateAlpList() {
        for (int i = 0; i < alphabetList.size(); i++) {
            final TextView testView = new TextView(this);
            testView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50));
            testView.setY(i * 50 + centerY);
            testView.setText(alphabetList.get(i));
            testView.setGravity(Gravity.CENTER);
            testView.setTextSize(20);
            alphabetContainer.addView(testView);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() ==  Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,  0, mMagnetometerReading.length);
        }

        updateOrientationAngles();
    }

    public void updateOrientationAngles() {
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

        View checkedView;
        boolean updateFlag = false;

        if (meanY < 0) {
            checkedView = alphabetContainer.getChildAt(alphabetContainer.getChildCount()-1);
            if ((checkedView.getY() - centerY) > 0) {
                updateFlag = true;
            }
        } else {
            checkedView = alphabetContainer.getChildAt(1);
            if ((checkedView.getY() - centerY) < 0) {
                updateFlag = true;
            }
        }

        if (updateFlag) {
            for (int i = 1; i < alphabetContainer.getChildCount(); i++) {
                View child = alphabetContainer.getChildAt(i);
                child.setY(child.getY() + (meanY * 5));
            }
        }
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
        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_GAME); // 20 ms, 50Hz
        mSensorManager.registerListener(this, mMagSensor, SensorManager.SENSOR_DELAY_GAME); // 20 ms, 50Hz
    }

    public class WordSuggestionTask extends AsyncTask<String, Void, List<String>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected List<String> doInBackground(String... params) {
            String input = params[0];
            List<String> wordList = lm.getWordsFromPrefix(input);
            List<String> ret = new ArrayList<>();
            for (int i = 0; i < suggestViewList.size(); i++) {
                if (i < wordList.size()) {
                    ret.add(wordList.get(i));
                } else {
                    ret.add("");
                }
            }
            return ret;
        }
        @Override
        protected void onPostExecute(final List<String> suggestedList) {
            super.onPostExecute(suggestedList);
            for (int i = 0; i < suggestViewList.size(); i++){
                suggestViewList.get(i).setText(suggestedList.get(i));
            }
        }
    }

    public class AlpSuggestionTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected String doInBackground(String... params) {
            String input = params[0];
            alphabetList = lm.getAlphasFromPrefix(input);
            return "done";
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }
    }

    public class LmInitTask extends AsyncTask<InputStream, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        protected String doInBackground(InputStream... params) {
            InputStream input = params[0];
            lm = new LanguageModel(input);
            return "done";
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            tempView.setText(result);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
