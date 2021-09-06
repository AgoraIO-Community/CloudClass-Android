package io.agora.edu.launch;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import io.agora.base.PreferenceManager;
import io.agora.education.api.EduCallback;
import io.agora.education.api.base.EduError;
import io.agora.education.api.logger.DebugItem;
import io.agora.education.api.manager.EduManager;

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

    public void checkUploadLog(EduManager manager) {
        if (!needUploadLog()) {
            return;
        }
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                manager.uploadDebugItem(DebugItem.LOG, new EduCallback<String>() {
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
