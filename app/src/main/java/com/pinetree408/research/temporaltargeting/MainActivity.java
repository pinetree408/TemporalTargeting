package com.pinetree408.research.temporaltargeting;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    ViewGroup alphabetContainer;
    int counter = 0;
    List<String> alphabetList;
    String[] initAlpList = {
            "t", "a", "i", "s", "o",
            "w", "c", "b", "h", "m",
            "p", "f", "d", "n", "r",
            "l", "e", "g", "y", "u",
            "k", "j", "v", "q", "x", "z"
    };
    private Timer mTimer;
    private TimerTask mTask;
    LanguageModel lm;

    TextView inputView;
    private List<TextView> suggestViewList;

    String nowInput;
    String preWords;

    String[] mackenzieSet;
    TextView targetView;
    Random random;

    Long startTime;

    ViewGroup taskView;
    private int dragThreshold = 30;
    private final double angleFactor = (double) 180/Math.PI;
    private float touchDownX, touchDownY;
    private long touchDownTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        alphabetContainer = (ViewGroup) findViewById(R.id.container);
        mTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        rotationItem();
                        counter++;
                        if (counter == 26) {
                            counter = 0;
                        }
                    }
                });
            }
        };
        mTimer = new Timer();
        mTimer.schedule(mTask, 3000, 500);

        final InputStream inputStream = getResources().openRawResource(R.raw.word_set);
        InputStream[] params = {inputStream};
        new LmInitTask().execute(params);

        alphabetList = Arrays.asList(initAlpList);

        inputView = (TextView) findViewById(R.id.input);
        suggestViewList = new ArrayList<>();
        suggestViewList.add((TextView) findViewById(R.id.suggest1));
        suggestViewList.add((TextView) findViewById(R.id.suggest2));
        suggestViewList.add((TextView) findViewById(R.id.suggest3));

        nowInput = "";
        preWords = "";

        mackenzieSet = Mackenzie.mackenzieSet;
        targetView = (TextView) findViewById(R.id.target);
        random = new Random();
        targetView.setText(mackenzieSet[random.nextInt(mackenzieSet.length)]);

        taskView = (ViewGroup) findViewById(R.id.task);
        taskView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                int tempX = (int) event.getAxisValue(MotionEvent.AXIS_X);
                int tempY = (int) event.getAxisValue(MotionEvent.AXIS_Y);
                long eventTime = System.currentTimeMillis();

                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (taskView.getClass() == v.getClass()) {
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
                                        break;
                                    case 1:
                                        //top;
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
                                        counter = 0;
                                        for (int i = 0; i < suggestViewList.size(); i++){
                                            suggestViewList.get(i).setText("");
                                        }
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
                            int selectedViewId = taskView.getId();
                            for (int i = 0; i < taskView.getChildCount(); i++) {
                                View child = taskView.getChildAt(i);
                                Rect bounds = new Rect();
                                child.getHitRect(bounds);
                                if (bounds.contains(tempX, tempY)) {
                                    selectedViewId = child.getId();
                                    break;
                                }
                            }
                            switch (selectedViewId) {
                                case R.id.container:
                                    if (startTime == null) {
                                        startTime = System.currentTimeMillis();
                                    }
                                    selectNearestItem();
                                    break;
                                case R.id.suggest1:case R.id.suggest2:case R.id.suggest3:
                                    TextView selectedView = (TextView) findViewById(selectedViewId);
                                    nowInput = "";
                                    if (preWords.length() != 0) {
                                        preWords = preWords + " " + selectedView.getText();
                                    } else {
                                        preWords = selectedView.getText().toString();
                                    }
                                    inputView.setText(preWords);
                                    alphabetList = Arrays.asList(initAlpList);
                                    counter = 0;
                                    for (int i = 0; i < suggestViewList.size(); i++){
                                        suggestViewList.get(i).setText("");
                                    }

                                    if (preWords.equals(targetView.getText())) {
                                        targetView.setText(mackenzieSet[random.nextInt(mackenzieSet.length)]);
                                        nowInput = "";
                                        preWords = "";
                                        inputView.setText("");
                                        double wpm = 5.0 / (((System.currentTimeMillis() - startTime) / 1000.0) / 60);
                                        System.out.println(wpm);
                                        startTime = System.currentTimeMillis();
                                    }
                                    break;
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }

    public void rotationItem() {
        final TextView testView = new TextView(this);
        testView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        testView.setText(alphabetList.get(counter));
        testView.setGravity(Gravity.CENTER);
        testView.setTextSize(20);

        ObjectAnimator testAni = ObjectAnimator.ofFloat(testView, "translationX", 320f, 0f);
        testAni.setDuration(3000);
        testAni.setInterpolator(new LinearInterpolator());
        testAni.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                testView.setX((Float)animation.getAnimatedValue());
            }
        });
        testAni.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                alphabetContainer.removeView(testView);
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        testAni.start();
        alphabetContainer.addView(testView, 0);
    }

    public void selectNearestItem() {
        int childCount = alphabetContainer.getChildCount();
        List<Float> distanceList = new ArrayList<>();
        for (int i = 0; i < childCount - 1; i++) {
            View child = alphabetContainer.getChildAt(i);
            float distance = Math.abs((alphabetContainer.getWidth() / 2.0f) - child.getX());
            distanceList.add(distance);
        }
        int minIndex = distanceList.indexOf(Collections.min(distanceList));
        TextView resultView = (TextView) alphabetContainer.getChildAt(minIndex);

        nowInput = nowInput + resultView.getText().toString();
        String[] params = {nowInput};
        new WordSuggestionTask().execute(params);
        new AlpSuggestionTask().execute(params);

        String visualizeString = "";
        if (preWords.length() != 0) {
            visualizeString = preWords + " " + nowInput;
        } else {
            visualizeString = nowInput;
        }
        inputView.setText(visualizeString);
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
            counter = 0;
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
        }
    }

    @Override
    protected void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
    }
}
