package io.agora.edu.classroom;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.ui.PlayerView;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.edu.R2;
import io.agora.edu.BuildConfig;
import io.agora.edu.R;
import io.agora.edu.base.BaseActivity;
import io.agora.edu.classroom.fragment.ReplayBoardFragment;

import static io.agora.edu.launch.AgoraEduSDK.VIDEO_URL;
import static io.agora.edu.launch.AgoraEduSDK.WHITEBOARD_APP_ID;
import static io.agora.edu.launch.AgoraEduSDK.WHITEBOARD_END_TIME;
import static io.agora.edu.launch.AgoraEduSDK.WHITEBOARD_ID;
import static io.agora.edu.launch.AgoraEduSDK.WHITEBOARD_START_TIME;
import static io.agora.edu.launch.AgoraEduSDK.WHITEBOARD_TOKEN;

public class ReplayActivity extends BaseActivity {
    private static final String TAG = "ReplayActivity";

    @BindView(R2.id.video_view)
    protected PlayerView video_view;

    private String whiteBoardAppId;
    private ReplayBoardFragment replayBoardFragment;
    private String url;
    private long startTime, endTime;
    private String boardId, boardToken;
    private boolean isInit;

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_replay;
    }

    @Override
    protected void initData() {
        Intent intent = getIntent();
        whiteBoardAppId = intent.getStringExtra(WHITEBOARD_APP_ID);
        url = intent.getStringExtra(VIDEO_URL);
        if (!url.startsWith("http")) {
            url = BuildConfig.REPLAY_BASE_URL.concat("/").concat(url);
        }
        Log.e(TAG, ":回放链接:" + url);
        startTime = intent.getLongExtra(WHITEBOARD_START_TIME, 0);
        endTime = intent.getLongExtra(WHITEBOARD_END_TIME, 0);
        boardId = intent.getStringExtra(WHITEBOARD_ID);
        boardToken = intent.getStringExtra(WHITEBOARD_TOKEN);
    }

    @Override
    protected void initView() {
        video_view.setUseController(false);
        video_view.setVisibility(!TextUtils.isEmpty(url) ? View.VISIBLE : View.GONE);
        findViewById(R.id.iv_temp).setVisibility(TextUtils.isEmpty(url) ? View.VISIBLE : View.GONE);

        replayBoardFragment = new ReplayBoardFragment(whiteBoardAppId);
        Bundle bundle = new Bundle();
        bundle.putLong(WHITEBOARD_START_TIME, startTime);
        bundle.putLong(WHITEBOARD_END_TIME, endTime);
        replayBoardFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_whiteboard, replayBoardFragment)
                .commitNow();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (!isInit) {
            replayBoardFragment.initReplayWithRoomToken(boardId, boardToken);
            replayBoardFragment.setPlayer(video_view, url);
            isInit = true;
        }
    }

    @Override
    protected void onDestroy() {
        replayBoardFragment.releaseReplay();
        super.onDestroy();
    }

    @OnClick(R2.id.iv_back)
    public void onClick(View view) {
        finish();
    }

}
