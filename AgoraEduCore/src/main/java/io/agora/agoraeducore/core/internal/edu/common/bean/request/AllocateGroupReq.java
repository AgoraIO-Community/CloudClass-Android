package io.agora.agoraeducore.core.internal.edu.common.bean.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.agoraeducore.core.internal.edu.common.bean.roompre.RoleConfig;

public class AllocateGroupReq {
    @NonNull
    private int memberLimit = 4;
    @Nullable
    private RoleConfig roleConfig;

    public AllocateGroupReq() {
    }
}
