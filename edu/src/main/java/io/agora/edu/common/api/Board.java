package io.agora.edu.common.api;

import io.agora.edu.common.bean.board.BoardBean;
import io.agora.education.api.EduCallback;

public interface Board {
    void requestBoardInfo(String userToken, EduCallback<BoardBean> callback);
}
