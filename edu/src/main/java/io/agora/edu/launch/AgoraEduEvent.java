package io.agora.edu.launch;

public enum AgoraEduEvent {
    AgoraEduEventFailed(0),
    AgoraEduEventReady(1),
    AgoraEduEventDestroyed(2),
    AgoraEduEventForbidden(3);

    private int value;

    public final int getValue() {
        return this.value;
    }

    public final void setValue(int var1) {
        this.value = var1;
    }

    private AgoraEduEvent(int value) {
        this.value = value;
    }
}