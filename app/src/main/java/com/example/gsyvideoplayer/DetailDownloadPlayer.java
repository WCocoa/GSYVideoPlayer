package com.example.gsyvideoplayer;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.danikula.videocache.HttpProxyCacheServer;
import com.example.gsyvideoplayer.databinding.ActivityDetailDownloadPlayerBinding;
import com.example.gsyvideoplayer.utils.MemoryCallBack;
import com.google.android.exoplayer2.SeekParameters;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager;
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack;
import com.shuyu.gsyvideoplayer.listener.GSYVideoProgressListener;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.request.RequestCall;

import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;


public class DetailDownloadPlayer extends AppCompatActivity {

    private boolean isPlay;
    private boolean isPause;

    private OrientationUtils orientationUtils;

    private HttpProxyCacheServer proxyCacheServer;
    private RequestCall requestCall;

    private ActivityDetailDownloadPlayerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailDownloadPlayerBinding.inflate(getLayoutInflater());

        View rootView = binding.getRoot();
        setContentView(rootView);


        String url = getUrl();

        //增加封面
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);

        resolveNormalVideoUI();

        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(this, binding.detailPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);

        Map<String, String> header = new HashMap<>();
        header.put("ee", "33");
        header.put("allowCrossProtocolRedirects", "true");
        GSYVideoOptionBuilder gsyVideoOption = new GSYVideoOptionBuilder();
        gsyVideoOption.setThumbImageView(imageView)
            .setIsTouchWiget(true)
            .setRotateViewAuto(false)
            .setLockLand(false)
            .setAutoFullWithSize(false)
            .setShowFullAnimation(false)
            .setNeedLockFull(true)
            .setUrl(url)
            .setMapHeadData(header)
            .setCacheWithPlay(true)
            .setVideoTitle("测试视频")
            .setVideoAllCallBack(new GSYSampleCallBack() {
                @Override
                public void onPrepared(String url, Object... objects) {
                    Debuger.printfError("***** onPrepared **** " + objects[0]);
                    Debuger.printfError("***** onPrepared **** " + objects[1]);
                    super.onPrepared(url, objects);
                    //开始播放了才能旋转和全屏
                    orientationUtils.setEnable(binding.detailPlayer.isRotateWithSystem());
                    isPlay = true;

                    //设置 seek 的临近帧。
                    if (binding.detailPlayer.getGSYVideoManager().getPlayer() instanceof Exo2PlayerManager) {
                        ((Exo2PlayerManager) binding.detailPlayer.getGSYVideoManager().getPlayer()).setSeekParameter(SeekParameters.NEXT_SYNC);
                        Debuger.printfError("***** setSeekParameter **** ");
                    }
                }

                @Override
                public void onEnterFullscreen(String url, Object... objects) {
                    super.onEnterFullscreen(url, objects);
                    Debuger.printfError("***** onEnterFullscreen **** " + objects[0]);//title
                    Debuger.printfError("***** onEnterFullscreen **** " + objects[1]);//当前全屏player
                }

                @Override
                public void onAutoComplete(String url, Object... objects) {
                    super.onAutoComplete(url, objects);
                }

                @Override
                public void onClickStartError(String url, Object... objects) {
                    super.onClickStartError(url, objects);
                }

                @Override
                public void onQuitFullscreen(String url, Object... objects) {
                    super.onQuitFullscreen(url, objects);
                    Debuger.printfError("***** onQuitFullscreen **** " + objects[0]);//title
                    Debuger.printfError("***** onQuitFullscreen **** " + objects[1]);//当前非全屏player

                    // ------- ！！！如果不需要旋转屏幕，可以不调用！！！-------
                    // 不需要屏幕旋转，还需要设置 setNeedOrientationUtils(false)
                    if (orientationUtils != null) {
                        orientationUtils.backToProtVideo();
                    }
                }
            })
            .setLockClickListener(new LockClickListener() {
                @Override
                public void onClick(View view, boolean lock) {
                    if (orientationUtils != null) {
                        //配合下方的onConfigurationChanged
                        orientationUtils.setEnable(!lock);
                    }
                }
            })
            .setGSYVideoProgressListener(new GSYVideoProgressListener() {
                @Override
                public void onProgress(long progress, long secProgress, long currentPosition, long duration) {
                    Debuger.printfLog(" progress " + progress + " secProgress " + secProgress + " currentPosition " + currentPosition + " duration " + duration);
                }
            })
            .build(binding.detailPlayer);

        binding.detailPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //直接横屏
                // ------- ！！！如果不需要旋转屏幕，可以不调用！！！-------
                // 不需要屏幕旋转，还需要设置 setNeedOrientationUtils(false)
                orientationUtils.resolveByClick();

                //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                binding.detailPlayer.startWindowFullscreen(DetailDownloadPlayer.this, true, true);
            }
        });


        binding.startDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = proxyCacheServer.getProxyUrl(getUrl());
                startDownload(url);

            }
        });

        binding.stopDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopDownload();
            }
        });

        proxyCacheServer = ProxyCacheManager.instance().newProxy(getApplicationContext());


    }

    @Override
    public void onBackPressed() {

        // ------- ！！！如果不需要旋转屏幕，可以不调用！！！-------
        // 不需要屏幕旋转，还需要设置 setNeedOrientationUtils(false)
        if (orientationUtils != null) {
            orientationUtils.backToProtVideo();
        }

        if (GSYVideoManager.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }


    @Override
    protected void onPause() {
        getCurPlay().onVideoPause();
        super.onPause();
        isPause = true;
    }

    @Override
    protected void onResume() {
        getCurPlay().onVideoResume(false);
        super.onResume();
        isPause = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isPlay) {
            getCurPlay().release();
        }
        stopDownload();
        if (orientationUtils != null)
            orientationUtils.releaseListener();
    }


    /**
     * orientationUtils 和  detailPlayer.onConfigurationChanged 方法是用于触发屏幕旋转的
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (isPlay && !isPause) {
            binding.detailPlayer.onConfigurationChanged(this, newConfig, orientationUtils, true, true);
        }
    }


    private void resolveNormalVideoUI() {
        //增加title
        binding.detailPlayer.getTitleTextView().setVisibility(View.GONE);
        binding.detailPlayer.getBackButton().setVisibility(View.GONE);
    }

    private GSYVideoPlayer getCurPlay() {
        if (binding.detailPlayer.getFullWindowPlayer() != null) {
            return binding.detailPlayer.getFullWindowPlayer();
        }
        return binding.detailPlayer;
    }


    private String getUrl() {

        String url = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

        return url;
    }

    private void startDownload(String url) {
        if (url == null || !url.startsWith("http")) {
            Toast.makeText(this, "URL 不是 Http 开头", Toast.LENGTH_SHORT).show();
            return;
        }
        //下载demo然后设置
        requestCall = OkHttpUtils.get().url(url)
            .build();
        requestCall.execute(new MemoryCallBack() {
            @Override
            public void onError(Call call, Exception e, int id) {
            }

            @Override
            public void onResponse(Boolean response, int id) {
                stopDownload();
            }

        });
    }

    private void stopDownload() {
        if (requestCall != null) {
            requestCall.cancel();
            requestCall = null;
        }
        if (proxyCacheServer != null) {
            proxyCacheServer.shutdown();
        }
    }
}
