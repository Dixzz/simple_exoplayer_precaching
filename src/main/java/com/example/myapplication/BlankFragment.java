package com.example.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import com.airbnb.lottie.LottieAnimationView;
import com.example.myapplication.databinding.FragmentBlankBinding;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.Util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link BlankFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BlankFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "BlankFragment";
    public Integer mParam1;
    long currentMillisec = 0;

    public VideoDataModel mParam2;
    private SimpleExoPlayer mPlayer;
    private MainActivity activity;
    @Nullable
    private FragmentBlankBinding fragmentBlankBinding;


    public BlankFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BlankFragment.
     */
    // TODO: Rename and change types and number of parameters
    @NonNull
    public static BlankFragment newInstance(Integer param1, VideoDataModel param2) {
        BlankFragment fragment = new BlankFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM1, param1);
        args.putSerializable(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    int onPauseVpIndex;
    int onResumeVpIndex;

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: " + mParam1 + " " + CommonUtil.getStateString(mPlayer.getPlaybackState()));
        if (mPlayer != null) {
            if (currentMillisec > 0)
                mPlayer.seekTo(currentMillisec);
            onResumeVpIndex = activity.viewPager.getCurrentItem();
            mPlayer.prepare();
            mPlayer.play();
            mPlayer.retry();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) {
            currentMillisec = mPlayer.getCurrentPosition();
            mPlayer.pause();
            mPlayer.stop();
            if (activity.viewPager != null) {
                onPauseVpIndex = activity.viewPager.getCurrentItem();
                if (activity.viewPager.getCurrentItem() != (mParam1 - 1)) {
                    // lastIndex is the swiped up video, lastIndex+1 is after swipe video which is already getting buffered/not buffered

                    int lastIndex = activity.viewPager.getCurrentItem(); // getCurrentItem() gives index+1
                    if (onPauseVpIndex > onResumeVpIndex)
                        activity.cacheVideo(lastIndex + 1);
                    else if (onPauseVpIndex < onResumeVpIndex)
                        activity.cacheVideo(lastIndex - 1);
                } else {
                    activity.stopCache();
                }
            }
        }
        Log.d(TAG, "onPause: " + onPauseVpIndex + " " + onResumeVpIndex);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }

    @Override
    public void onDestroyView() {
        mPlayer.stop(true);
        mPlayer.release();
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mPlayer != null) {
            Toast.makeText(activity, "Low memory pausing video player", Toast.LENGTH_SHORT).show();
            mPlayer.stop();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity) requireActivity();

        if (getArguments() != null) {
            mParam1 = getArguments().getInt(ARG_PARAM1);
            mParam2 = (VideoDataModel) getArguments().getSerializable(ARG_PARAM2);
        }

        CustomLoad loadControl = new CustomLoad.Builder().build();

        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(activity, activity.getPackageName()));

        DataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(activity.simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        TrackSelector trackSelector = new DefaultTrackSelector(activity, new AdaptiveTrackSelection.Factory());
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(activity).setResetOnNetworkTypeChange(true).setInitialBitrateEstimate(Util.getUserAgent(activity, activity.getPackageName())).build();

        if (mPlayer == null)
            mPlayer = new SimpleExoPlayer.Builder(activity).setTrackSelector(trackSelector)
                    .setBandwidthMeter(bandwidthMeter)
                    .setUseLazyPreparation(true)
                    .setLoadControl(loadControl)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory)).build();
        mPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        mPlayer.setMediaSource(new ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(MediaItem.fromUri(mParam2.video)));
        mPlayer.setMediaItem(MediaItem.fromUri(mParam2.video));
        mPlayer.addListener(new Player.Listener() {

            @Override
            public void onPlayerError(@NotNull ExoPlaybackException error) {
                // FIXME: 7/18/2021 For some reason when onDestroy() of activity is called playback is stuck and doesn't restart
                Log.d(TAG, "onPlayerError: " + error.getMessage());
                if (error.type == ExoPlaybackException.TYPE_SOURCE) {

                    Log.d(TAG, "onPlayerError: " + mPlayer);
                    Log.d(TAG, "onPlayerError: " + mParam2.video);
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                Log.d(TAG, "onPlaybackStateChanged: " + CommonUtil.getStateString(state) + " " + mParam1);

                if (activity.viewPager != null && mParam1 - 1 != activity.viewPager.getCurrentItem()) {
                    Log.d(TAG, "onPlaybackStateChanged: " + "Total buffered: " + TimeUnit.MILLISECONDS.toSeconds(mPlayer.getTotalBufferedDuration()) + "sec Total duration: " + TimeUnit.MILLISECONDS.toSeconds(mPlayer.getDuration()) + "sec " + " div " + mParam1 + " " + mPlayer.getBufferedPercentage() + "%" + " ");
                    if (TimeUnit.MILLISECONDS.toSeconds(mPlayer.getTotalBufferedDuration()) >= 1) {
                        mPlayer.stop();
                    }
                }

                if (activity.viewPager != null && mParam1 - 1 == activity.viewPager.getCurrentItem()) {
                    if (state == Player.STATE_BUFFERING) {
                        activity.indicator.setIndeterminate(true);
                        activity.indicator.setVisibility(View.VISIBLE);
                    } else if (state == Player.STATE_READY) {
                        //cacheUsingSecMethod();
                        activity.indicator.setIndeterminate(false);
                        activity.indicator.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build();
        mPlayer.setAudioAttributes(audioAttributes, true);
        mPlayer.prepare();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        fragmentBlankBinding = DataBindingUtil.bind(inflater.inflate(R.layout.fragment_blank, container, false));
        fragmentBlankBinding.setData(mParam2);
        /*fragmentBlankBinding.playerView.setOnTouchListener(new View.OnTouchListener() {
            Context context;
            private final GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    return true;
                }
            });

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                view.performClick();
                return true;
            }
        });*/
        return fragmentBlankBinding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        VideoDataModel v = mParam2;

        if (mParam1 - 1 == 0) {
            mPlayer.setPlayWhenReady(true);
            Log.d(TAG, "onViewCreated: "+mParam1);
            //Log.d(TAG, "run: " + activity.adapter.videoDataModels.get(0).count.like_count);
            Log.d(TAG, "run: " + mParam2.count.like_count);
            new Handler().postDelayed(() -> {
                v.count.like_count = 1000d;
                fragmentBlankBinding.setData(v);
                //activity.adapter.notifyItemChanged(0);

                Log.d(TAG, "run: 2 " + mParam2.count.like_count);
                Log.d(TAG, "run: 3 " + fragmentBlankBinding.getData().count.like_count);
            }, 5000);
        }
        fragmentBlankBinding.playerView.setControllerAutoShow(false);
        mPlayer.seekTo(0, 0);
        fragmentBlankBinding.playerView.setPlayer(mPlayer);
        fragmentBlankBinding.profileImage.setImageURI(mParam2.user_info.profile_pic);
        /*new Thread() {
            @Override
            public void run() {
                String s = CommonUtil.humanReadableByteCountSI(fragmentBlankBinding.getData().count.like_count);
                activity.runOnUiThread(() -> fragmentBlankBinding.likeCounter.setText(s));
            }
        }.start();*/
        fragmentBlankBinding.lottieLikes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull View view) {
                if (fragmentBlankBinding.lottieLikes.getProgress() == 0)
                    ((LottieAnimationView) view).playAnimation();
            }
        });
    }

    /*private void cacheUsingSecMethod() {
        if (mParam2._cacheKey != null) {
            Log.d(TAG, "startCaching: pre " + activity.simpleCache.getKeys().size() + " Position: " + (activity.viewPager.getCurrentItem() + 1) + " " + mParam1);
            activity.simpleCache.getCachedSpans(mParam2._cacheKey).forEach(s -> {
                Log.d(TAG, "startCaching: " + s.isCached + " " + new Date(s.lastTouchTimestamp) + s);

                try {
                    activity.simpleCache.startReadWriteNonBlocking(s.key, s.position, s.length);
                } catch (Cache.CacheException e) {
                    e.printStackTrace();
                }
            });
        } else {
            Log.d(TAG, "startCaching: new " + activity.simpleCache.getKeys().size() + " Position: " + activity.viewPager.getCurrentItem());
            activity.simpleCache.getKeys().forEach(e -> {
                mParam2.setCacheKey(e);
                activity.simpleCache.getCachedSpans(e).forEach(s -> {
                    Log.d(TAG, "startCaching: " + s.isCached + " " + new Date(s.lastTouchTimestamp));
                    try {
                        activity.simpleCache.startReadWriteNonBlocking(s.key, s.position, s.length);
                    } catch (Cache.CacheException cacheException) {
                        cacheException.printStackTrace();
                    }
                });
            });
        }
    }*/
}