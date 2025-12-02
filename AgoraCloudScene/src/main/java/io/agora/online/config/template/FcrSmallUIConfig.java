package io.agora.online.config.template;

import io.agora.online.config.FcrUIConfig;

/**
 * author : felix
 * date : 2022/7/11
 * description :
 * <p>
 * area
 * - isHeaderVisible
 * - isStageVisible
 * - isEngagementVisible
 */
public class FcrSmallUIConfig extends FcrUIConfig {
    // 修改值
    public FcrSmallUIConfig() {
        isHeaderVisible = true;
        isStageVisible = true;
        isEngagementVisible = true;
        isExtensionVisible = true;

        stateBar.networkState.isVisible = true;
        stateBar.scheduleTime.isVisible = true;
        stateBar.roomName.isVisible = true;

        teacherVideo.resetPosition.isVisible = true;
        teacherVideo.offStage.isVisible = true;
        teacherVideo.privateChat.isVisible = true;

        studentVideo.offStage.isVisible = true;
        studentVideo.privateChat.isVisible = true;
        studentVideo.boardAuthorization.isVisible = true;
        studentVideo.camera.isVisible = true;
        studentVideo.microphone.isVisible = true;
        studentVideo.reward.isVisible = true;

        popupQuiz.isVisible = true;
        cloudStorage.isVisible = true;
        raiseHand.isVisible = true;
        roster.isVisible = true;

        netlessBoard.clear.isVisible = true;
        netlessBoard.eraser.isVisible = true;
        netlessBoard.mouse.isVisible = true;
        netlessBoard.pencil.isVisible = true;
        netlessBoard.save.isVisible = true;
        netlessBoard.selector.isVisible = true;
        netlessBoard.text.isVisible = true;

        agoraChat.isVisible = true;
        agoraChat.muteAll.isVisible = true;
        agoraChat.emoji.isVisible = true;
        agoraChat.picture.isVisible = true;
    }
}
