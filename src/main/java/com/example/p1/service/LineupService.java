package com.example.p1.service;

import java.io.IOException;

public interface LineupService {
    void crawlAndSaveLineups(Long gameScheduleId) throws IOException;
}