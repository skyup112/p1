package com.example.p1.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Log4j2
public class GameController {
}
