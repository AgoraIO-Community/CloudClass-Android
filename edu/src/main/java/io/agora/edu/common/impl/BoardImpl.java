package io.agora.edu.common.impl;

import org.jetbrains.annotations.NotNull;

import io.agora.edu.common.api.Base;
import io.agora.edu.common.bean.board.BoardBean;
import io.agora.edu.common.api.Board;
import io.agora.education.api.EduCallback;

public class BoardImpl extends Base implements Board {
    private static final String TAG = "BoardImpl";

    public BoardImpl(@NotNull String appId, @NotNull String roomUuid) {
        super(appId, roomUuid);
    }

    @Override
    public void requestBoardInfo(String userToken, EduCallback<BoardBean> callback) {

    }
}
