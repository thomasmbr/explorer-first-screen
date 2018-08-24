package com.lapic.thomas.explorador_primeira_tela;

import android.animation.LayoutTransition;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;

import com.lapic.thomas.explorador_primeira_tela.network.MulticastGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener, CustomVideoView.PlayPauseListener, MediaPlayer.OnCompletionListener {

    @BindView(R.id.scroll_view) protected ScrollView scrollView;
    @BindView(R.id.rl_video) protected RelativeLayout rl_video;
    @BindView(R.id.video_view) protected CustomVideoView videoView;
    @BindView(R.id.rl_details) protected RelativeLayout rl_details;

    private final String TAG = this.getClass().getSimpleName();
    private MediaController mediaController;
    private int position = 0;
    private int width;
    private int height;
    private MulticastGroup multicastGroup;
    private String tag_multicast = "first_screen";
    private String ip_multicast = "230.192.0.10";
    private int port_multicast = 1027;
    private Synchronizer synchronizer;

    private List<String> colors = Arrays.asList("RED", "GREEN", "YELLOW", "BLUE");
    private List<Integer> anchors = Arrays.asList(19, 23, 47, 57, 82, 90, 95, 99, 115);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        width = displayMetrics.widthPixels + 144;
        height = displayMetrics.heightPixels ;

        Log.e(TAG, "deviceWidth: " + width);
        Log.e(TAG, "deviceHeight: "+ height);

        prepareVideo();
        setFullScreenVideo();
        videoView.start();

        new CountDownTimer(5000, 5000){
            @Override public void onTick(long millisUntilFinished) { }
            @Override public void onFinish() {
                MainActivity.this.setMinimizeVideo();
            }
        }.start();

        new CountDownTimer(30000, 30000){
            @Override public void onTick(long millisUntilFinished) { }
            @Override public void onFinish() {
                MainActivity.this.setFullScreenVideo();
            }
        }.start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Store current position.
        savedInstanceState.putInt("CurrentPosition", videoView.getCurrentPosition());
        videoView.pause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Get saved position.
        position = savedInstanceState.getInt("CurrentPosition");
        videoView.seekTo(position);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (multicastGroup == null)
            multicastGroup = new MulticastGroup(this, tag_multicast, ip_multicast, port_multicast);
        multicastGroup.startMessageReceiver();
        if (synchronizer != null)
            synchronizer.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (synchronizer != null)
            synchronizer.pause();
    }

    // METHODS

    public void prepareVideo() {
        if (mediaController == null)
            mediaController = new MediaController(this, false);
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.video ;
        mediaController.setAnchorView(videoView);
        mediaController.setMediaPlayer(videoView);
        videoView.setOnPreparedListener(this);
        videoView.setPlayPauseListener(this);
        videoView.setOnCompletionListener(this);
        videoView.setMediaController(mediaController);
        videoView.setVideoURI(Uri.parse(videoPath));
    }

    public void setFullScreenVideo() {
        scrollView.setVisibility(View.GONE);
        rl_details.setVisibility(View.GONE);

        RelativeLayout.LayoutParams layoutParamsRLVideo = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rl_video.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        rl_video.setLayoutParams(layoutParamsRLVideo);
        rl_video.setGravity(Gravity.CENTER);
    }

    public void setMinimizeVideo() {
        RelativeLayout.LayoutParams relativeLayoutParamDetails = new RelativeLayout.LayoutParams(width/2, height/2);
        relativeLayoutParamDetails.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayoutParamDetails.addRule(RelativeLayout.ALIGN_PARENT_END);
        relativeLayoutParamDetails.addRule(RelativeLayout.ALIGN_RIGHT);
        rl_details.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        rl_details.setLayoutParams(relativeLayoutParamDetails);
        rl_details.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams layoutParamsRLVideo = new RelativeLayout.LayoutParams(width/2, height/2);
        layoutParamsRLVideo.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParamsRLVideo.addRule(RelativeLayout.ALIGN_PARENT_END);
        layoutParamsRLVideo.addRule(RelativeLayout.ALIGN_RIGHT);
        rl_video.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        rl_video.setLayoutParams(layoutParamsRLVideo);
        rl_video.setGravity(Gravity.CENTER);

        scrollView.setLayoutParams(new RelativeLayout.LayoutParams(width/2, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollView.setVisibility(View.VISIBLE);
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("START","drawing");
            jsonObject.put("duration", (videoView.getDuration()/1000));
            String message = URLEncoder.encode(jsonObject.toString(), "UTF-8");
            if (multicastGroup != null) {
                multicastGroup.sendMessage(false, message);
                createActions();
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        int topContainerId = getResources().getIdentifier("mediacontroller_progress", "id", "android");
        SeekBar seekBarVideo = (SeekBar) mediaController.findViewById(topContainerId);
        seekBarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { seekBar.setEnabled(false); }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { seekBar.setEnabled(false); }
        });
    }

    public void createActions() throws JSONException {
        int i = 0;
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (Integer time : anchors) {
            if (i >= 4)
                i = 0;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time", time);
            jsonObject.put("START", colors.get(i));
            jsonObject.put("id", i);
            jsonObjects.add(jsonObject);
            i++;
        }
        synchronizer = new Synchronizer(jsonObjects);
        synchronizer.start();
    }

    @Override
    public void onPlayVideo() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("RESUME","drawing");
            String message = URLEncoder.encode(jsonObject.toString(), "UTF-8");
            if (multicastGroup != null)
                multicastGroup.sendMessage(false, message);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPauseVideo() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("PAUSE","drawing");
            String message = URLEncoder.encode(jsonObject.toString(), "UTF-8");
            if (multicastGroup != null)
                multicastGroup.sendMessage(false, message);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("STOP","drawing");
            String message = URLEncoder.encode(jsonObject.toString(), "UTF-8");
            if (multicastGroup != null)
                multicastGroup.sendMessage(false, message);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    public class Synchronizer extends Thread {

        private Handler handler1;
        private List<JSONObject> listActions;
        private boolean shouldPlay = true;

        public Synchronizer(List<JSONObject> listActions) {
            this.listActions = listActions;
            this.handler1 = new Handler();
        }

        @Override
        public void run() {
            for (final JSONObject action : listActions) {
                try {
                    handler1.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                            String message = URLEncoder.encode(action.toString(), "UTF-8");
                            if (multicastGroup != null)
                                multicastGroup.sendMessage(false, message);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, action.getInt("time") * 1000);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        public void play() {
            this.shouldPlay = true;
        }

        public void pause() {
            this.shouldPlay = false;
        }

    }

}
