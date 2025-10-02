package com.example.p1.service;

import com.example.p1.domain.Team;
import com.example.p1.dto.TeamDTO; // Import TeamDTO

import java.util.List;
import java.util.Optional;

public interface TeamService {
    List<TeamDTO> getAllTeams(); // Return DTOs
    Optional<TeamDTO> getTeamById(Long id); // Return DTO
    Optional<TeamDTO> getTeamByName(String name); // Return DTO
    TeamDTO createTeam(TeamDTO teamDTO); // Accept DTO
    TeamDTO updateTeam(Long id, TeamDTO updatedTeamDTO); // Accept DTO, return DTO
    void deleteTeam(Long id);
}