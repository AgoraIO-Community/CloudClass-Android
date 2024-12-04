package com.hyphenate.easeimgroup.modules.view.ui.emoji;


import io.agora.agoraeduuikit.R;
import com.hyphenate.easeimgroup.modules.utils.EaseSmileUtils;

public class EaseDefaultEmojiconDatas {
    
    private static String[] emojis = new String[]{
        EaseSmileUtils.ee_1,
        EaseSmileUtils.ee_2,
        EaseSmileUtils.ee_3,
        EaseSmileUtils.ee_4,
        EaseSmileUtils.ee_5,
        EaseSmileUtils.ee_6,
        EaseSmileUtils.ee_7,
        EaseSmileUtils.ee_8,
        EaseSmileUtils.ee_9,
        EaseSmileUtils.ee_10,
        EaseSmileUtils.ee_11,
        EaseSmileUtils.ee_12,
        EaseSmileUtils.ee_13,
        EaseSmileUtils.ee_14,
        EaseSmileUtils.ee_15,
        EaseSmileUtils.ee_16,
        EaseSmileUtils.ee_17,
        EaseSmileUtils.ee_18,
        EaseSmileUtils.ee_19,
        EaseSmileUtils.ee_20,
        EaseSmileUtils.ee_21,
        EaseSmileUtils.ee_22,
        EaseSmileUtils.ee_23,
        EaseSmileUtils.ee_24,
        EaseSmileUtils.ee_25,
        EaseSmileUtils.ee_26,
        EaseSmileUtils.ee_27,
        EaseSmileUtils.ee_28,
        EaseSmileUtils.ee_29,
        EaseSmileUtils.ee_30,
        EaseSmileUtils.ee_31,
        EaseSmileUtils.ee_32,
        EaseSmileUtils.ee_33,
        EaseSmileUtils.ee_34,
        EaseSmileUtils.ee_35,
       
    };
    
    private static int[] icons = new int[]{
        R.mipmap.ee_1,
        R.mipmap.ee_2,
        R.mipmap.ee_3,
        R.mipmap.ee_4,
        R.mipmap.ee_5,
        R.mipmap.ee_6,
        R.mipmap.ee_7,
        R.mipmap.ee_8,
        R.mipmap.ee_9,
        R.mipmap.ee_10,
        R.mipmap.ee_11,
        R.mipmap.ee_12,
        R.mipmap.ee_13,
        R.mipmap.ee_14,
        R.mipmap.ee_15,
        R.mipmap.ee_16,
        R.mipmap.ee_17,
        R.mipmap.ee_18,
        R.mipmap.ee_19,
        R.mipmap.ee_20,
        R.mipmap.ee_21,
        R.mipmap.ee_22,
        R.mipmap.ee_23,
        R.mipmap.ee_24,
        R.mipmap.ee_25,
        R.mipmap.ee_26,
        R.mipmap.ee_27,
        R.mipmap.ee_28,
        R.mipmap.ee_29,
        R.mipmap.ee_30,
        R.mipmap.ee_31,
        R.mipmap.ee_32,
        R.mipmap.ee_33,
        R.mipmap.ee_34,
        R.mipmap.ee_35,
    };
    
    
    private static final EaseEmojicon[] DATA = createData();
    
    private static EaseEmojicon[] createData(){
        EaseEmojicon[] datas = new EaseEmojicon[icons.length];
        for(int i = 0; i < icons.length; i++){
            datas[i] = new EaseEmojicon(icons[i], emojis[i]);
        }
        return datas;
    }
    
    public static EaseEmojicon[] getData(){
        return DATA;
    }
}
