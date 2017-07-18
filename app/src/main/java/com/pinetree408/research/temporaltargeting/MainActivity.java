package com.pinetree408.research.temporaltargeting;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    ViewGroup alphabetContainer;
    int counter = 0;
    List<String> alphabetList;
    private Timer mTimer;
    private TimerTask mTask;
    LanguageModel lm;

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
        mTimer.schedule(mTask, 500, 500);
        View task = findViewById(R.id.task);
        task.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        selectNearestItem();
                        break;
                }
                return false;
            }
        });
        lm = new LanguageModel();
        alphabetList = lm.getAlphasFromPrefix("");
    }

    public void rotationItem() {
        final TextView testView = new TextView(this);
        testView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        testView.setText(alphabetList.get(counter));
        testView.setGravity(Gravity.CENTER);
        testView.setTextSize(30);

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
        TextView inputView = (TextView) findViewById(R.id.input);
        int childCount = alphabetContainer.getChildCount();
        List<Float> distanceList = new ArrayList<>();
        for (int i = 0; i < childCount - 1; i++) {
            View child = alphabetContainer.getChildAt(i);
            float distance = Math.abs((alphabetContainer.getWidth() / 2.0f) - child.getX());
            distanceList.add(distance);
        }
        int minIndex = distanceList.indexOf(Collections.min(distanceList));
        TextView resultView = (TextView) alphabetContainer.getChildAt(minIndex);
        inputView.setText(inputView.getText().toString() + resultView.getText().toString());
    }

    @Override
    protected void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
    }
}
