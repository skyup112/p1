package com.example.p1.domain; // or com.example.p1.domain.enums;

public enum GameStatus {
    SCHEDULED,   // Game is planned
    IN_PROGRESS, // Game is currently being played (추가)
    FINISHED,    // Game has concluded
    CANCELED     // Game was cancelled
}