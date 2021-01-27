package io.agora.edu.classroom.widget.chat;

public class ChatItem {
    public enum ItemType {
        Chat, System
    }

    public ItemType type;
    public String message;

    // For chat type only
    public String translate;
    public boolean translated;
}