package io.agora.online.impl.whiteboard.netless.annotation;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@StringDef({
        com.herewhite.sdk.domain.Appliance.SELECTOR,
        com.herewhite.sdk.domain.Appliance.PENCIL,
        com.herewhite.sdk.domain.Appliance.RECTANGLE,
        com.herewhite.sdk.domain.Appliance.ELLIPSE,
        com.herewhite.sdk.domain.Appliance.ERASER,
        com.herewhite.sdk.domain.Appliance.TEXT,
        com.herewhite.sdk.domain.Appliance.ARROW,
        com.herewhite.sdk.domain.Appliance.STRAIGHT,
        com.herewhite.sdk.domain.Appliance.LASER_POINTER
})
@Retention(RetentionPolicy.SOURCE)
public @interface Appliance {

}
