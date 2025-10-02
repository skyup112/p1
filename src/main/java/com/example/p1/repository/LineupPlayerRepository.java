package com.example.p1.repository;

import com.example.p1.domain.LineupPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineupPlayerRepository extends JpaRepository<LineupPlayer, Long> {
    List<LineupPlayer> findByGameLineupIdOrderByOrderNumberAsc(Long gameLineupId);
    void deleteByGameLineupId(Long gameLineupId);// Optional: for explicit deletion
    List<LineupPlayer> findByPlayerRoleIsNull();
}