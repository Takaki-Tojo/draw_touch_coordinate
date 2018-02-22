package com.slpl.drawcoordinate.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WordData {
    // 文字毎
    private long wordId = 1;
    private long deleteWordId = 0;
    private double inputTime = 0;
    private String inputText = "";
    private int cursorPosition = 0;
    private String date = "";
    private Integer deleteCount = 0;
    private int missDelete = 0;
    private String detectedWord = null;
    private int touchDownX = 0;
    private int touchDownY = 0;
    private int touchUpX = 0;
    private int touchUpY = 0;
    private int missTouchX = 0;
    private int missTouchY = 0;
}
