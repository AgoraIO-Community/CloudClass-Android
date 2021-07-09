package io.agora.education.rtmtoken;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
