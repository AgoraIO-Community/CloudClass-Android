package io.agora.edu.common.bean.request;

public class RaiseHandReq {
    String payload;

    public RaiseHandReq(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
