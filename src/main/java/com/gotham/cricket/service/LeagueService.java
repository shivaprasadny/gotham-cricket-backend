package com.gotham.cricket.service;

import com.gotham.cricket.dto.CreateLeagueRequest;
import com.gotham.cricket.dto.LeagueResponse;
import com.gotham.cricket.entity.League;
import com.gotham.cricket.repository.LeagueRepository;
import com.gotham.cricket.repository.MatchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final MatchRepository matchRepository;

    // Create new league
    public String createLeague(CreateLeagueRequest request) {
        League league = new League();
        league.setName(request.getName());
        league.setSeason(request.getSeason());
        league.setType(request.getType());
        league.setDescription(request.getDescription());
        league.setStartDate(request.getStartDate());
        league.setEndDate(request.getEndDate());
        league.setActive(request.isActive());

        leagueRepository.save(league);

        return "League created successfully";
    }

    // Return all leagues
    public List<LeagueResponse> getAllLeagues() {
        return leagueRepository.findAllByOrderByActiveDescNameAsc()
                .stream()
                .map(league -> new LeagueResponse(
                        league.getId(),
                        league.getName(),
                        league.getSeason(),
                        league.getType(),
                        league.getDescription(),
                        league.getStartDate(),
                        league.getEndDate(),
                        league.isActive()
                ))
                .toList();
    }

    // Get one league
    public LeagueResponse getLeagueById(Long id) {
        League league = leagueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("League not found"));

        return new LeagueResponse(
                league.getId(),
                league.getName(),
                league.getSeason(),
                league.getType(),
                league.getDescription(),
                league.getStartDate(),
                league.getEndDate(),
                league.isActive()
        );
    }

    // Update existing league
    public String updateLeague(Long id, CreateLeagueRequest request) {
        League league = leagueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("League not found"));

        league.setName(request.getName());
        league.setSeason(request.getSeason());
        league.setType(request.getType());
        league.setDescription(request.getDescription());
        league.setStartDate(request.getStartDate());
        league.setEndDate(request.getEndDate());
        league.setActive(request.isActive());

        leagueRepository.save(league);

        return "League updated successfully";
    }

    // Delete league
    @Transactional
    public String deleteLeague(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new RuntimeException("League not found"));

        boolean usedInMatches = matchRepository.existsByLeagueId(leagueId);

        if (usedInMatches) {
            throw new RuntimeException("Cannot delete league. League is used in one or more matches.");
        }

        leagueRepository.delete(league);

        return "League deleted successfully";
    }
}