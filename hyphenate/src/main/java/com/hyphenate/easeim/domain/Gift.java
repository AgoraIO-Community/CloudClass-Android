package com.hyphenate.easeim.domain;

/**
 * 礼物类
 */
public class Gift {
    private String name;
    private String img;
    private String score;
    private String desc;

    public String getDesc() {
        return desc;
    }

    public Gift(){}

    public Gift(String name, String img, String score, String desc) {
        this.name = name;
        this.img = img;
        this.score = score;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getImg() {
        return img;
    }

    public String getScore() {
        return score;
    }
}
