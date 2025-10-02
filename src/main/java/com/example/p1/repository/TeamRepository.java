
package com.example.p1.repository;

import com.example.p1.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // This method is crucial for looking up the Team entity by its name
    Team findByName(String name);
}