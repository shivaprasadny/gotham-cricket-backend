package com.gotham.cricket.repository;

import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.TeamMember;
import com.gotham.cricket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByTeam(Team team);
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    List<TeamMember> findByTeamId(Long teamId);
}