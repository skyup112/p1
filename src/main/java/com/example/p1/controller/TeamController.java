package com.example.p1.controller;

import com.example.p1.dto.TeamDTO; // Import TeamDTO
import com.example.p1.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    /**
     * Get all teams.
     * GET /api/teams
     * @return A list of all TeamDTO objects.
     */
    @GetMapping
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        List<TeamDTO> teams = teamService.getAllTeams();
        return ResponseEntity.ok(teams);
    }

    /**
     * Get a team by ID.
     * GET /api/teams/{id}
     * @param id The ID of the team (Long).
     * @return The TeamDTO object if found, or 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamDTO> getTeamById(@PathVariable Long id) {
        Optional<TeamDTO> team = teamService.getTeamById(id);
        return team.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get a team by Name.
     * GET /api/teams/by-name/{name}
     * @param name The name of the team (String).
     * @return The TeamDTO object if found, or 404 Not Found.
     */
    @GetMapping("/by-name/{name}") // New endpoint for lookup by name
    public ResponseEntity<TeamDTO> getTeamByName(@PathVariable String name) {
        Optional<TeamDTO> team = teamService.getTeamByName(name);
        return team.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new team.
     * POST /api/teams
     * @param teamDTO The TeamDTO object to create (from request body).
     * @return The created TeamDTO object with its generated ID, and 201 Created status.
     */
    @PostMapping
    public ResponseEntity<TeamDTO> createTeam(@RequestBody TeamDTO teamDTO) {
        TeamDTO createdTeam = teamService.createTeam(teamDTO);
        return new ResponseEntity<>(createdTeam, HttpStatus.CREATED);
    }

    /**
     * Update an existing team.
     * PUT /api/teams/{id}
     * @param id The ID of the team to update.
     * @param updatedTeamDTO The TeamDTO object with updated data (from request body).
     * @return The updated TeamDTO object, or 404 Not Found if the team doesn't exist.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeamDTO> updateTeam(@PathVariable Long id, @RequestBody TeamDTO updatedTeamDTO) {
        try {
            TeamDTO team = teamService.updateTeam(id, updatedTeamDTO);
            return ResponseEntity.ok(team);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a team by ID.
     * DELETE /api/teams/{id}
     * @param id The ID of the team to delete.
     * @return 204 No Content if successful, or 404 Not Found if the team doesn't exist.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        try {
            teamService.deleteTeam(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}