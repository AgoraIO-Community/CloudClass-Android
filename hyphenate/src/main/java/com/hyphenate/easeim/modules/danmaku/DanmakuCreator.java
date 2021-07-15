package com.hyphenate.easeim.modules.danmaku;

import android.content.Context;

import com.hyphenate.chat.EMCustomMessageBody;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.easeim.R;
import com.hyphenate.easeim.constant.DemoConstant;
import com.hyphenate.easeim.utils.RandomUtil;
import com.hyphenate.easeim.utils.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;


public class DanmakuCreator {
    private static final String TAG = "DanmakuCreator";
    private char[] mCharArr;
    private String[] mColors = {
            "#FFFFFFFF", "#FFFF0000", "#FFFFFF00", "#FF00FF00"
    };
    public DanmakuCreator(){}
//    public DanmakuCreator(Context context) {
//        InputStream is = context.getResources().openRawResource(R.raw.word);
//        BufferedReader br = new BufferedReader(new InputStreamReader(is));
//        String s = null;
//        try {
//            s = br.readLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if (s == null || s.length() == 0) {
//            s = "赵钱孙李周吴郑王";
//        }
//        mCharArr = s.toCharArray();
//    }

    public Danmaku create(EMMessage message) {
        Danmaku danmaku = new Danmaku(message.getMsgId());
        if (message.getType() == EMMessage.Type.TXT) {
            EMTextMessageBody body = (EMTextMessageBody) message.getBody();
            danmaku.text = body.getMessage();
        } else if (message.getType() == EMMessage.Type.CUSTOM) {
            EMCustomMessageBody body = (EMCustomMessageBody) message.getBody();
            Map<String, String> params = body.getParams();
            danmaku.text = params.get("des");
            danmaku.giftUrl = params.get("url");
            danmaku.avatarUrl = message.getStringAttribute(DemoConstant.AVATAR_URL, "");
        }
        danmaku.mode = Danmaku.Mode.scroll;
        danmaku.size = ScreenUtil.autoSize(40);
        danmaku.color = randomColor();
        return danmaku;
    }

//    public Danmaku create() {
//        Danmaku danmaku = new Danmaku();
//        danmaku.text = randomText();
//        danmaku.mode = Danmaku.Mode.scroll;
//        danmaku.color = randomColor();
//        danmaku.size = ScreenUtil.autoSize(56, 36);
//
//        return danmaku;
//    }

//    private String randomText() {
//        int length = (int) (26 - Math.sqrt(RandomUtil.nextInt(625)));
//
//        int arrLen = mCharArr.length;
//        StringBuilder s = new StringBuilder();
//        while (length > 0) {
//            char c = mCharArr[RandomUtil.nextInt(arrLen)];
//            s.append(c);
//            length--;
//        }
//
//        return s.toString();
//    }

    private String randomColor() {
        int i = RandomUtil.nextInt(10);
        if (i <= 5) {
            return "#FF000000";
        } else if (i == 6) {
            return "#FF0000FF";
        } else if (i == 7) {
            return "#FFFF0000";
        } else if (i == 8) {
            return "#FFFFFF00";
        } else {
            return "#FF00FF00";
        }
    }
}
