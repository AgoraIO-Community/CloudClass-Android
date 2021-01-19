package io.agora.edu.classroom.bean.channel;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.agora.base.bean.JsonBean;

public class Room extends JsonBean {
    @Type
    public int type;

    @IntDef({Type.ONE2ONE, Type.SMALL, Type.LARGE, Type.BREAKOUT, Type.INTERMEDIATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
        int ONE2ONE = 0;
        int SMALL = 1;
        int LARGE = 2;
        int BREAKOUT = 3;
        int INTERMEDIATE = 4;
    }

}
