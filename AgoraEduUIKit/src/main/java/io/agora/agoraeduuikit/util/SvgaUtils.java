package io.agora.agoraeduuikit.util;

import android.content.Context;
import android.util.Log;

import com.opensource.svgaplayer.SVGACallback;
import com.opensource.svgaplayer.SVGADrawable;
import com.opensource.svgaplayer.SVGAImageView;
import com.opensource.svgaplayer.SVGAParser;
import com.opensource.svgaplayer.SVGAVideoEntity;

import java.util.ArrayList;

/**
 * SVGA工具类
 * 使用时首先调用初始化数据方法，
 * 然后再调用开始动画的方法
 */

public class SvgaUtils {
    private Context context;
    private ArrayList<String> stringList;
    private SVGAImageView svgaImage;
    private SVGAParser parser;

    public SvgaUtils(Context context, SVGAImageView svgaImage) {
        this.context = context;
        this.svgaImage = svgaImage;
    }

    /**
     * 初始化数据
     */
    public void initAnimator() {
        parser = new SVGAParser(context);
        stringList = new ArrayList<>();
    }

    /**
     * 显示动画
     */
    public void startAnimator(String svgaName) {
        stringList.add(stringList.size(), svgaName + ".svga");
        parseSVGA();
    }

    /**
     * 停止动画
     */
    public void stopSVGA() {
        if (svgaImage.isAnimating()) {
            svgaImage.stopAnimation();
        }
    }

    /**
     * 解析加载动画
     */
    private void parseSVGA() {
        try {
            parser.parse(stringList.get(0), new SVGAParser.ParseCompletion() {
                @Override
                public void onComplete(SVGAVideoEntity svgaVideoEntity) {
                    //解析动画成功，到这里才真正的显示动画
                    SVGADrawable drawable = new SVGADrawable(svgaVideoEntity);
                    svgaImage.setImageDrawable(drawable);
                    svgaImage.startAnimation();
                }

                @Override
                public void onError() {
                }
            });
        } catch (Exception e) {
        }

    }
}