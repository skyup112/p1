package com.example.p1.service;

import com.example.p1.dto.RegisterRequestDTO;

public interface AuthService {
    void registerNewUser(RegisterRequestDTO registerRequest);
}