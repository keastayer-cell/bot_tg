package com.example.bot;

import java.io.*;
import java.util.*;

public class QueueItem {
    public enum Type { TEXT, IMAGE }
    
    public Type type;
    public String content; // текст или имя файла картинки
    
    public QueueItem(Type type, String content) {
        this.type = type;
        this.content = content;
    }
    
    @Override
    public String toString() {
        return type + ":" + content;
    }
    
    public static QueueItem fromString(String s) {
        String[] parts = s.split(":", 2);
        if (parts.length != 2) return null;
        try {
            Type type = Type.valueOf(parts[0]);
            return new QueueItem(type, parts[1]);
        } catch (Exception e) {
            return null;
        }
    }
}
