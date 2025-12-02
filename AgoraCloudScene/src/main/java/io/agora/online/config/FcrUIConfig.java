package io.agora.online.config;

import io.agora.online.config.component.FcrAgoraChatUIConfig;
import io.agora.online.config.component.FcrBreakoutRoomUIConfig;
import io.agora.online.config.component.FcrCounterUIConfig;
import io.agora.online.config.component.FcrNetlessBoardUIConfig;
import io.agora.online.config.component.FcrPollUIConfig;
import io.agora.online.config.component.FcrPopupQuizUIConfig;
import io.agora.online.config.component.FcrScreenShareUIConfig;
import io.agora.online.config.component.FcrStateBarUIConfig;
import io.agora.online.config.component.FcrStudentVideoUIConfig;
import io.agora.online.config.component.FcrTeacherVideoUIConfig;
import io.agora.online.config.component.FcrCloudStorageUIConfig;
import io.agora.online.config.component.FcrRaiseHandUIConfig;
import io.agora.online.config.component.FcrRosterUIConfig;

/**
 * author : felix
 * date : 2022/7/11
 * description :
 * 定义：https://confluence.agoralab.co/pages/viewpage.action?pageId=1034322973
 */
public abstract class FcrUIConfig {
    /**
     * 头部区域
     */
    public boolean isHeaderVisible = true;
    /**
     * 上台区域(视频区域)
     */
    public boolean isStageVisible = true;
    /**
     * 白板区域
     */
    public boolean isEngagementVisible = true;
    /**
     * 聊天区域
     */
    public boolean isExtensionVisible = true;

    public FcrStateBarUIConfig stateBar = new FcrStateBarUIConfig();
    public FcrTeacherVideoUIConfig teacherVideo = new FcrTeacherVideoUIConfig();
    public FcrStudentVideoUIConfig studentVideo = new FcrStudentVideoUIConfig();
    public FcrPopupQuizUIConfig popupQuiz = new FcrPopupQuizUIConfig();
    public FcrCounterUIConfig counter = new FcrCounterUIConfig();
    public FcrPollUIConfig poll = new FcrPollUIConfig();
    public FcrCloudStorageUIConfig cloudStorage = new FcrCloudStorageUIConfig();
    public FcrScreenShareUIConfig screenShare = new FcrScreenShareUIConfig();
    public FcrBreakoutRoomUIConfig breakoutRoom = new FcrBreakoutRoomUIConfig();
    public FcrRaiseHandUIConfig raiseHand = new FcrRaiseHandUIConfig();
    public FcrRosterUIConfig roster = new FcrRosterUIConfig();
    public FcrNetlessBoardUIConfig netlessBoard = new FcrNetlessBoardUIConfig();
    public FcrAgoraChatUIConfig agoraChat = new FcrAgoraChatUIConfig();
}
