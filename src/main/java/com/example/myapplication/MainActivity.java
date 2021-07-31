package com.example.myapplication;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.TraceCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheWriter;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_PERCENTAGE_TO_PRE_CACHE = 10;
    private static final String TAG = "MainActivity";
    @Nullable
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient();
    private static final String URL_LINK = "http://fatema.takatakind.com/app_api/index.php?p=showAllVideos";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public SimpleCache simpleCache;
    @Nullable
    @BindView(R.id.indicator)
    public LinearProgressIndicator indicator;
    @Nullable
    @BindView(R.id.viewpager2)
    ViewPager2 viewPager;
    //@BindView(R.id.viewpager2)
    int max_tries = 1;
    @Nullable
    @BindView(R.id.lottieStart)
    LottieAnimationView startLoader;
    @Nullable
    private ArrayList<VideoDataModel> videoDataModels = new ArrayList<>();
    private Viewpager2Adapter adapter;
    private CacheDataSource cacheDataSourceFactory;
    @Nullable
    private CacheWriter cacheWriter;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public void onBackPressed() {
        Snackbar.make(indicator, "Exit the app?", Snackbar.LENGTH_LONG).setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE).setAction("Yes", view -> MainActivity.super.onBackPressed()).show();
    }

    private void stopLottie() {
        startLoader.cancelAnimation();
        startLoader.animate().scaleX(0f).scaleY(0f).setDuration(500).start();
    }

    private void startLottie() {
        if (!isDestroyed()) {
            startLoader.playAnimation();
            startLoader.animate().scaleY(1f).scaleX(1f).setDuration(750).start();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (simpleCache == null)
            simpleCache = ((MyApp) getApplication()).simpleCache;

        Window window = getWindow();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            window.setDecorFitsSystemWindows(false);
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.getDecorView().setOnApplyWindowInsetsListener((view, windowInsets) -> {
            viewPager.setPaddingRelative(0, 0, 0, windowInsets.getSystemWindowInsetBottom());
            return windowInsets;
        });

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        startLottie();
        viewPager.postDelayed(() -> {
            viewPager.setVisibility(View.VISIBLE);
        }, 3000);

        viewPager.setOffscreenPageLimit(2); // Three fragments will be loaded each time, by default it loads n+1, where n=1
        if (adapter == null) {
            adapter = new Viewpager2Adapter(getSupportFragmentManager(), getLifecycle(), viewPager);
            viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
            viewPager.setAdapter(adapter);
        }

        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        HttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);

        cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .createDataSource();
        fetchVideos();
    }

    private void fetchVideos() {
        postguy(URL_LINK, "", new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    JSONArray jsonArray = new JSONArray(jsonObject.getString("msg"));

                    videoDataModels = VideoDataModel.getListFromString(jsonArray.toString());
                    if (adapter.getItemCount() == 0) {
                        adapter.setVideoDataModels(videoDataModels);
                        handler.post(adapter::notifyDataSetChanged);
                    }

                    cacheVideo(0);
                    //cacheVideo(Math.min(videoDataModels.size(), 3));
                    jsonArray = null;
                    jsonObject = null;
                    handler.post(MainActivity.this::stopLottie);
                } catch (ConnectException e) {
                    handler.post(() -> Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show());
                    finish();
                } catch (@NonNull JSONException | SocketTimeoutException ignored) {
                    max_tries++;
                    if (max_tries < 4) {
                        handler.post(() -> Toast.makeText(MainActivity.this, "Retrying " + max_tries + "/3 times", Toast.LENGTH_SHORT).show());
                        postguy(URL_LINK, "", this);
                    } else
                        finish();
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(MainActivity.this::stopLottie);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(TAG, "onFailure: " + startLoader.getSpeed());
                handler.post(() -> {
                    Snackbar.make(viewPager, "" + e.getLocalizedMessage(), Snackbar.LENGTH_INDEFINITE).setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE).setAction("Retry", view -> fetchVideos()).show();
                });
            }
        });
    }

    public void stopCache() {
        if (cacheWriter != null) {
            Log.e(TAG, "stopCache: ");
            cacheWriter.cancel();
        }
    }

    public void cacheVideo(Integer itemsToFetchUpto) {
        if (itemsToFetchUpto >= 0 && itemsToFetchUpto < videoDataModels.size()) {
            VideoDataModel dataModel = videoDataModels.get(itemsToFetchUpto);
            Log.d(TAG, "cacheVideo: starting cache for " + itemsToFetchUpto + "Current vp index: " + viewPager.getCurrentItem());
            CacheWriter.ProgressListener progressListener = new CacheWriter.ProgressListener() {
                @Override
                public void onProgress(long requestLength, long bytesCached, long newBytesCached) {
                    if (requestLength > 0 && bytesCached > 0) {
                        float req = Float.parseFloat(requestLength + "");
                        float downl = Float.parseFloat(bytesCached + "");
                        int percentage = (int) ((downl / req) * 100);
                        if (percentage == MAX_PERCENTAGE_TO_PRE_CACHE) {
                            if (cacheWriter != null) {
                                cacheWriter.cancel();
                            }
                        }
                    }
                }
            };
            cacheWriter = new CacheWriter(cacheDataSourceFactory, new DataSpec(Uri.parse(dataModel.video)), null, progressListener);
            executor.execute(() -> {
                startCaching(dataModel, progressListener);
            });
        }

        //for (VideoDataModel model : videoDataModels) {
        /*CacheWriter.ProgressListener progressListener = new CacheWriter.ProgressListener() {
            @Override
            public void onProgress(long requestLength, long bytesCached, long newBytesCached) {
                if (requestLength > 0 && bytesCached > 0) {
                    float req = Float.parseFloat(requestLength + "");
                    float downl = Float.parseFloat(bytesCached + "");
                    int percentage = (int) ((downl / req) * 100);
                    if (percentage == MAX_PERCENTAGE_TO_CACHE) {
                        cacheWriter.cancel();
                    }
                    Log.d(TAG, "onProgress: " + percentage);
                }
            }
        };

        executor.execute(() -> {
            cacheWriter = new CacheWriter(cacheDataSourceFactory, new DataSpec(Uri.parse(model.video)), null, progressListener);
            try {
                cacheWriter.cache();
            } catch (InterruptedIOException ignored) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        });*/
        //}
    }

    private void startCaching(@NonNull VideoDataModel dataModel, CacheWriter.ProgressListener progressListener) {

        try {
            if (cacheWriter == null) {
                cacheWriter = new CacheWriter(cacheDataSourceFactory, new DataSpec(Uri.parse(dataModel.video)), null, progressListener);
            } else {
                cacheWriter.cache();
            }
        } catch (@NonNull HttpDataSource.HttpDataSourceException | NullPointerException | InterruptedIOException ignored) {
        } catch (@NonNull IllegalStateException | ArrayIndexOutOfBoundsException ignored) {
            cacheWriter.cancel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void postguy(@NonNull String url, @NonNull String bodyJson, @NonNull Callback callback) {
        RequestBody body = RequestBody.create(bodyJson, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    static class Viewpager2Adapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        FragmentManager fragmentManager;
        ViewPager2 viewPager2;

        /*@Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean containsItem(long itemId) {
            if (videoDataModels != null) {
                return videoDataModels.contains(videoDataModels.get((int) itemId));
            }
            return false;
        }

        @Override
        public void onBindViewHolder(@NonNull FragmentViewHolder holder, int position, @NonNull List<Object> payloads) {
            String tag = "f" + holder.getItemId();

            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            Log.d(TAG, "onBindViewHolder: " + fragment);
            if (fragment != null) {
                ((BlankFragment) fragment).currentMillisec = 0;
                ((BlankFragment) fragment).mParam2.count.like_count = 10000d;
                Log.d(TAG, "onBindViewHolder: " + videoDataModels.get(position).count.like_count + " aa");

            } else {
                super.onBindViewHolder(holder, position, payloads);
            }
        }*/

        private ArrayList<VideoDataModel> videoDataModels;

        public void setVideoDataModels(ArrayList<VideoDataModel> videoDataModels) {
            this.videoDataModels = videoDataModels;
        }

        public Viewpager2Adapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, ViewPager2 viewPager2) {
            super(fragmentManager, lifecycle);
            this.fragmentManager = fragmentManager;
            this.viewPager2 = viewPager2;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return BlankFragment.newInstance((position + 1), videoDataModels.get(position));
        }

        @Override
        public int getItemCount() {
            if (videoDataModels == null) return 0;
            else return videoDataModels.size();
        }
    }

    static class ViewPagerAdapter extends FragmentStatePagerAdapter {
        private ArrayList<VideoDataModel> videoDataModels;

        public ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        public void setVideoDataModels(ArrayList<VideoDataModel> videoDataModels) {
            this.videoDataModels = videoDataModels;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return BlankFragment.newInstance((position + 1), videoDataModels.get(position));
        }

        @Override
        public int getCount() {
            if (videoDataModels == null) return 0;
            else return videoDataModels.size();
        }
    }
}