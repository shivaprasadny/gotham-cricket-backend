package com.gotham.cricket.service;

import com.gotham.cricket.dto.TeamMemberResponse;
import com.gotham.cricket.dto.TeamRequest;
import com.gotham.cricket.dto.TeamResponse;
import com.gotham.cricket.entity.MemberProfile;
import com.gotham.cricket.entity.Team;
import com.gotham.cricket.entity.TeamMember;
import com.gotham.cricket.entity.User;
import com.gotham.cricket.repository.MemberProfileRepository;
import com.gotham.cricket.repository.TeamMemberRepository;
import com.gotham.cricket.repository.TeamRepository;
import com.gotham.cricket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;

    public String createTeam(TeamRequest request) {
        if (teamRepository.existsByTeamName(request.getTeamName())) {
            throw new RuntimeException("Team name already exists");
        }

        Team team = new Team();
        team.setTeamName(request.getTeamName());
        team.setDescription(request.getDescription());

        if (request.getCaptainId() != null) {
            User captain = userRepository.findById(request.getCaptainId())
                    .orElseThrow(() -> new RuntimeException("Captain not found with id: " + request.getCaptainId()));
            team.setCaptain(captain);
        }

        teamRepository.save(team);
        return "Team created successfully";
    }

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(team -> new TeamResponse(
                        team.getId(),
                        team.getTeamName(),
                        team.getDescription(),
                        team.getCaptain() != null ? team.getCaptain().getId() : null,
                        team.getCaptain() != null ? team.getCaptain().getFullName() : null
                ))
                .toList();
    }

    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        return new TeamResponse(
                team.getId(),
                team.getTeamName(),
                team.getDescription(),
                team.getCaptain() != null ? team.getCaptain().getId() : null,
                team.getCaptain() != null ? team.getCaptain().getFullName() : null
        );
    }

    public String addMemberToTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (teamMemberRepository.findByTeamAndUser(team, user).isPresent()) {
            throw new RuntimeException("User is already in this team");
        }

        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(user);

        teamMemberRepository.save(teamMember);

        return "Member added to team successfully";
    }

    public String removeMemberFromTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        TeamMember teamMember = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new RuntimeException("User is not part of this team"));

        teamMemberRepository.delete(teamMember);

        return "Member removed from team successfully";
    }

    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + teamId));

        return teamMemberRepository.findByTeam(team)
                .stream()
                .map(teamMember -> {
                    User user = teamMember.getUser();
                    MemberProfile profile = memberProfileRepository.findByUser(user).orElse(null);

                    return new TeamMemberResponse(
                            teamMember.getId(),
                            user.getId(),
                            user.getFullName(),
                            user.getEmail(),
                            profile != null ? profile.getNickname() : null,
                            profile != null ? profile.getPlayerType() : null,
                            profile != null ? profile.getJerseyNumber() : null,
                            teamMember.getJoinedAt()
                    );
                })
                .toList();
    }
}