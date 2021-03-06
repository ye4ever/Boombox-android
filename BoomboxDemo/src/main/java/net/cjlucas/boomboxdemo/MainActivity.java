package net.cjlucas.boomboxdemo;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import net.cjlucas.boombox.Boombox;
import net.cjlucas.boombox.BoomboxInfoListener;
import net.cjlucas.boombox.provider.*;

public class MainActivity extends Activity
        implements
        BoomboxInfoListener,
        RadioGroup.OnCheckedChangeListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MainActivity";
    private static final int UPDATE_UI_INTERVAL = 100;

    private Timer mUpdateUiTimer;

    private Button mPrevButton;
    private Button mNextButton;
    private Button mPlayPauseButton;
    private TextView mCurrentlyPlayingTextView;
    private TextView mUpNextTextView;
    private TextView mProgressTextView;
    private TextView mDurationTextView;
    private RadioGroup mContinuousRadioGroup;
    private SeekBar mSeekBar;
    private boolean mIsSeeking;
    private int mSeekProgress;


    private class UpdateUiTimerTask extends TimerTask {
        public void run() {
            updateUi();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("MainActivity: onCreate");
        BoomboxSingleton.getInstance().setInfoListener(this);
        if (BoomboxSingleton.getInstance().getPlaylist().size() > 0) return;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.sources)));
            String line = null;

            while (true) {
                line = in.readLine();
                if (line == null) {
                    break;
                }

                URL url = new URL(line);
                String fileName = url.toString().substring(
                        url.toString().lastIndexOf("/") + 1);
                System.out.println(fileName);
                BoomboxSingleton.getInstance().addProvider(new HttpAudioDataProvider(url, fileName));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Log.i(TAG, "onPostCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "onStart");
        mPrevButton = (Button) findViewById(R.id.prev_btn);
        mNextButton = (Button) findViewById(R.id.next_btn);
        mPlayPauseButton = (Button) findViewById(R.id.play_pause_btn);
        mCurrentlyPlayingTextView = (TextView) findViewById(R.id.currently_playing);
        mUpNextTextView = (TextView) findViewById(R.id.up_next);
        mProgressTextView = (TextView) findViewById(R.id.progress);
        mDurationTextView = (TextView) findViewById(R.id.duration);

        mContinuousRadioGroup = (RadioGroup) findViewById(R.id.continuous_group);
        mContinuousRadioGroup.setOnCheckedChangeListener(this);

        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mIsSeeking = false;

        System.out.println(mUpdateUiTimer);

        mUpdateUiTimer = new Timer();
        mUpdateUiTimer.schedule(new UpdateUiTimerTask(), 0, UPDATE_UI_INTERVAL);
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        mUpdateUiTimer.cancel();
        mUpdateUiTimer = null;
        getBoombox().setInfoListener(null);

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public Boombox getBoombox() {
        return BoomboxSingleton.getInstance();
    }

    public List<AudioDataProvider> getProviders() {
        return getBoombox().getProviders();

    }

    private void updateUi() {
        runOnUiThread(new Runnable() {
            public void run() {
                mPrevButton.setEnabled(getBoombox().hasPrevious());
                mNextButton.setEnabled(getBoombox().hasNext());

                double currentPosition = getBoombox().getCurrentPosition() / 1000.0;
                double duration = getBoombox().getDuration() / 1000.0;

                AudioDataProvider currentProvider = getBoombox().getCurrentProvider();

                String currentlyPlaying = String.format(Locale.getDefault(), "%s: %s",
                        getResources().getString(R.string.currently_playing),
                        currentProvider == null ? "None" : currentProvider.getId());

                mCurrentlyPlayingTextView.setText(currentlyPlaying);

                AudioDataProvider nextProvider = getBoombox().getNextProvider();

                String upNext = String.format(Locale.getDefault(), "%s: %s",
                        getResources().getString(R.string.up_next),
                        nextProvider == null ? "None" : nextProvider.getId());

                mUpNextTextView.setText(upNext);

                mProgressTextView.setText(sformat("%s: %fs",
                        getResources().getString(R.string.progress), currentPosition));

                mDurationTextView.setText(sformat("%s: %fs",
                        getResources().getString(R.string.duration), duration));

                switch (getBoombox().getContinuousMode()) {
                    case NONE:
                        mContinuousRadioGroup.check(R.id.continuous_none);
                        break;
                    case SINGLE:
                        mContinuousRadioGroup.check(R.id.continuous_single);
                        break;
                    case PLAYLIST:
                        mContinuousRadioGroup.check(R.id.continuous_playlist);
                        break;
                }

                if (!mIsSeeking) {
                    mSeekBar.setProgress(duration > 0
                            ? (int)((currentPosition / duration) * 100) : 0);
                }
            }
        });
    }

    public void togglePlayPauseClicked(View v) {
        getBoombox().togglePlayPause();
        updateUi();
    }

    public void playNextClicked(View view) {
        getBoombox().playNext();
        updateUi();
    }

    public void playPreviousClicked(View view) {
        getBoombox().playPrevious();
        updateUi();
    }

    public void shuffleModeSwitchChanged(View view) {
        getBoombox().setShuffleMode(((Switch) view).isChecked());
    }

    public void showPlaylist(View view) {
        Intent intent = new Intent(this, ProviderListActivity.class);
        startActivity(intent);
    }

    private String sformat(String format, Object...args) {
        return String.format(Locale.getDefault(), format, args);
    }

    // BoomboxInfoListener Methods

    @Override
    public void onPlaybackStart(Boombox boombox, AudioDataProvider provider) {
        Log.i(TAG, "onPlaybackStart");
        updateUi();
    }

    @Override
    public void onPlaybackCompletion(Boombox boombox,
                                     AudioDataProvider completedProvider, AudioDataProvider nextProvider) {
        Log.i(TAG, "onPlaybackCompletion");
        updateUi();
    }

    @Override
    public void onPlaylistCompletion(Boombox boombox) {
        Log.i(TAG, "onPlaylistCompletion");
        updateUi();
    }

    public void onBufferingUpdate(Boombox boombox, AudioDataProvider provider, int percentComplete) {
        //Log.i(TAG, "onBufferingUpdate: " + percentComplete);
    }

    public void onBufferingStart(Boombox boombox, AudioDataProvider provider) {
        Log.i(TAG, "onBufferingStart");
    }

    public void onBufferingEnd(Boombox boombox, AudioDataProvider provider) {
        Log.i(TAG, "onBufferingEnd");
    }

    // RadioGroup.OnCheckedChangeListener method
    public void onCheckedChanged(RadioGroup radioGroup, int id) {
        Boombox.ContinuousMode mode = null;

        switch (id) {
            case R.id.continuous_none:
                mode = Boombox.ContinuousMode.NONE;
                break;
            case R.id.continuous_single:
                mode = Boombox.ContinuousMode.SINGLE;
                break;
            case R.id.continuous_playlist:
                mode = Boombox.ContinuousMode.PLAYLIST;
                break;
            default:
                break;
        }

        getBoombox().setContinuousMode(mode);
        updateUi();
    }

    // SeekBar.OnSeekBarChangeListener methods

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        Log.v(TAG, sformat("onProgressChanged: progress: %d fromUser: %s",
//                progress, fromUser ? "YES" : "NO"));

        if (fromUser) mSeekProgress = progress;
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsSeeking = true;
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        mIsSeeking = false;
    }
}
