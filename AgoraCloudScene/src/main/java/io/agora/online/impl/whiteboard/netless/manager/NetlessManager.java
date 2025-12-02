package io.agora.online.impl.whiteboard.netless.manager;

import com.herewhite.sdk.domain.Promise;
import com.herewhite.sdk.domain.SDKError;

public abstract class NetlessManager<T> {
    protected T t;
    protected Promise<T> promise = new Promise<T>() {
        @Override
        public void then(T t) {
            NetlessManager.this.t = t;
            onSuccess(t);
        }

        @Override
        public void catchEx(SDKError t) {
            onFail(t);
        }
    };

    public abstract void onSuccess(T t);

    public abstract void onFail(SDKError error);
}
