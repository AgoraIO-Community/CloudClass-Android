package io.agora.online.config.template;

import io.agora.online.config.FcrUIConfig;

/**
 * author : felix
 * date : 2022/7/11
 * description : default ui config
 */
public class FcrDefUIConfig extends FcrUIConfig {
    public FcrDefUIConfig() {
        isHeaderVisible = true;
        isExtensionVisible = true;
        isEngagementVisible = true;
        isStageVisible = true;

        stateBar.networkState.isVisible = true;
        stateBar.scheduleTime.isVisible = true;
        stateBar.roomName.isVisible = true;

        teacherVideo.resetPosition.isVisible = true;
        teacherVideo.offStage.isVisible = true;

        studentVideo.offStage.isVisible = true;
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
