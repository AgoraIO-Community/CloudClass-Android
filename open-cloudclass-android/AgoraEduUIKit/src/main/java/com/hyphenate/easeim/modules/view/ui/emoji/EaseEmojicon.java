package com.hyphenate.easeimgroup.modules.view.ui.emoji;

public class EaseEmojicon {
    public EaseEmojicon(){
    }
    
    /**
     * constructor
     * @param icon- resource id of the icon
     * @param emojiText- text of emoji icon
     */
    public EaseEmojicon(int icon, String emojiText){
        this.icon = icon;
        this.emojiText = emojiText;
    }

    /**
     * identity code
     */
    private String identityCode;
    
    /**
     * static icon resource id
     */
    private int icon;

    /**
     * text of emoji, could be null for big icon
     */
    private String emojiText;
    
    /**
     * name of emoji icon
     */
    private String name;
    
    /**
     * path of icon
     */
    private String iconPath;
    
    /**
     * path of big icon
     */
    private String bigIconPath;
    
    
    /**
     * get the resource id of the icon
     * @return
     */
    public int getIcon() {
        return icon;
    }


    /**
     * set the resource id of the icon
     * @param icon
     */
    public void setIcon(int icon) {
        this.icon = icon;
    }

    /**
     * get text of emoji icon
     * @return
     */
    public String getEmojiText() {
        return emojiText;
    }


    /**
     * set text of emoji icon
     * @param emojiText
     */
    public void setEmojiText(String emojiText) {
        this.emojiText = emojiText;
    }

    /**
     * get name of emoji icon
     * @return
     */
    public String getName() {
        return name;
    }
    
    /**
     * set name of emoji icon
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get icon path
     * @return
     */
    public String getIconPath() {
        return iconPath;
    }


    /**
     * set icon path
     * @param iconPath
     */
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    /**
     * get identity code
     * @return
     */
    public String getIdentityCode() {
        return identityCode;
    }
    
    /**
     * set identity code
     * @param identityCode
     */
    public void setIdentityCode(String identityCode) {
        this.identityCode = identityCode;
    }

    public static String newEmojiText(int codePoint) {
        if (Character.charCount(codePoint) == 1) {
            return String.valueOf(codePoint);
        } else {
            return new String(Character.toChars(codePoint));
        }
    }

}
