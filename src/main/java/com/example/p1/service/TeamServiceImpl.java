// src/main/java/com/example/p1/service/TeamServiceImpl.java
package com.example.p1.service;

import com.example.p1.domain.Team;
import com.example.p1.dto.TeamDTO; // Import TeamDTO
import com.example.p1.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;

    // Helper method to convert Entity to DTO
    private TeamDTO convertToDto(Team team) {
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .logoUrl(team.getLogoUrl())
                .build();
    }

    // Helper method to convert DTO to Entity (for creation/update)
    private Team convertToEntity(TeamDTO teamDTO) {
        return Team.builder()
                .id(teamDTO.getId()) // ID might be null for new creation
                .name(teamDTO.getName())
                .logoUrl(teamDTO.getLogoUrl())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamDTO> getTeamById(Long id) {
        return teamRepository.findById(id)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamDTO> getTeamByName(String name) {
        return Optional.ofNullable(teamRepository.findByName(name))
                .map(this::convertToDto);
    }

    @Override
    public TeamDTO createTeam(TeamDTO teamDTO) {
        Team team = convertToEntity(teamDTO);
        if (team.getId() != null) {
            System.err.println("경고: Team 생성 시 ID가 제공되었습니다. ID가 자동으로 생성됩니다.");
            team.setId(null); // Explicitly nullify to ensure sequence generation
        }
        Team savedTeam = teamRepository.save(team);
        return convertToDto(savedTeam); // Convert saved Entity back to DTO
    }

    @Override
    public TeamDTO updateTeam(Long id, TeamDTO updatedTeamDTO) {
        Team existingTeam = teamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다: " + id));

        existingTeam.setName(updatedTeamDTO.getName());
        existingTeam.setLogoUrl(updatedTeamDTO.getLogoUrl());

        Team savedTeam = teamRepository.save(existingTeam);
        return convertToDto(savedTeam); // Convert updated Entity back to DTO
    }

    @Override
    public void deleteTeam(Long id) {
        if (!teamRepository.existsById(id)) {
            throw new IllegalArgumentException("삭제할 팀을 찾을 수 없습니다: " + id);
        }
        // TODO: Consider adding logic here to handle associated entities (e.g., GameSchedule)
        // If GameSchedule has a foreign key to Team, you might need to set opponentTeam to null
        // or delete related GameSchedules, depending on your business rules and CASCADE settings.
        teamRepository.deleteById(id);
    }
}