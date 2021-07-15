package io.agora.edu.common.bean.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.edu.common.bean.roompre.RoleConfig;

public class AllocateGroupReq {
    @NonNull
    private int memberLimit = 4;
    @Nullable
    private RoleConfig roleConfig;

    public AllocateGroupReq() {
    }
}
