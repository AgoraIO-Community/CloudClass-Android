package io.agora.agoraeducore.core.internal.util;

public class ColorUtil {

    public static int[] colorToArray(int color) {
        int[] colorArray = new int[3];
        colorArray[0] = color >> 16 & 0xFF;
        colorArray[1] = color >> 8 & 0xFF;
        colorArray[2] = color & 0xFF;
        return colorArray;
    }


    public static int converRgbToArgb(int[] rgb) {
        if(rgb.length != 3) {
            return -65536;
        }
        int color = ((0xFF << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]);
        return color;
    }
}
