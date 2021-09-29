package io.agora.agoraeducore.core.internal.launch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.education.api.logger.DebugItem;
import io.agora.agoraeducore.core.internal.framework.EduManager;
import io.agora.agoraeducore.core.internal.framework.data.EduCallback;
import io.agora.agoraeducore.core.internal.framework.data.EduError;

public class AgoraEduLog {
    private static final String TAG = "AgoraEduLog";
    private final String uploadLog = "need-upload-log";

    private Timer timer;
    private TimerTask task;

    public void writeLogSign(boolean upload) {
        PreferenceManager.put(uploadLog, upload);
    }

    private boolean needUploadLog() {
        Boolean need = PreferenceManager.get(uploadLog, false);
        need = need == null ? false : need;
        return need;
    }

    public void checkUploadLog(EduManager manager, Object payload) {
        if (!needUploadLog()) {
            return;
        }
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                manager.uploadDebugItem(DebugItem.LOG, payload, new EduCallback<String>() {
                    @Override
                    public void onSuccess(@Nullable String res) {
                        writeLogSign(false);
                        releaseTask();
                    }

                    @Override
                    public void onFailure(@NotNull EduError error) {
                        releaseTask();
                    }
                });
            }
        };
        timer.schedule(task, 30 * 1000);
    }

    private void releaseTask() {
        if (task != null) {
            task.cancel();
        }
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }
        task = null;
        timer = null;
    }
}
