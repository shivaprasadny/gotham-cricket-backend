package com.gotham.cricket.controller;

import com.gotham.cricket.dto.AvailableTeamMemberResponse;
import com.gotham.cricket.dto.TeamMemberResponse;
import com.gotham.cricket.dto.TeamRequest;
import com.gotham.cricket.dto.TeamResponse;
import com.gotham.cricket.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/teams", "/api/v1/teams"})
@RequiredArgsConstructor
@Tag(
        name = "Teams",
        description = "Create and manage teams, team details, and team memberships"
)
public class TeamController {

    private final TeamService teamService;

    // Admin only - create new team
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Create a team",
            description = "Creates a new team and optionally assigns a captain. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Team created successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "Team created successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request, duplicate team name, or captain not found"
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public String createTeam(@Valid @RequestBody TeamRequest request) {
        return teamService.createTeam(request);
    }

    // Everyone can view teams
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(
            summary = "Get all teams",
            description = "Returns all teams. Requires the ADMIN, CAPTAIN, or PLAYER role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Teams retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TeamResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Required role not granted")
    })
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams();
    }

    // Everyone can view single team details
    @GetMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(
            summary = "Get a team by ID",
            description = "Returns details for one team. Requires the ADMIN, CAPTAIN, or PLAYER role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Team retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TeamResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Team not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Required role not granted")
    })
    public TeamResponse getTeamById(
            @Parameter(description = "Unique team ID", example = "1", required = true)
            @PathVariable Long teamId
    ) {
        return teamService.getTeamById(teamId);
    }

    // Captain/Admin can add members to team
    @PostMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(
            summary = "Add a member to a team",
            description = "Adds an existing user to a team. Requires the ADMIN or CAPTAIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Member added successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "Member added to team successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Team or user not found, or user is already a team member"
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or CAPTAIN role required")
    })
    public String addMemberToTeam(
            @Parameter(description = "Unique team ID", example = "1", required = true)
            @PathVariable Long teamId,
            @Parameter(description = "Unique user ID", example = "12", required = true)
            @PathVariable Long userId
    ) {
        return teamService.addMemberToTeam(teamId, userId);
    }

    // Captain/Admin can remove members from team
    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(
            summary = "Remove a member from a team",
            description = "Removes an existing member from a team. Requires the ADMIN or CAPTAIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Member removed successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "Member removed from team successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Team or user not found, or user is not a member of the team"
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or CAPTAIN role required")
    })
    public String removeMemberFromTeam(
            @Parameter(description = "Unique team ID", example = "1", required = true)
            @PathVariable Long teamId,
            @Parameter(description = "Unique user ID", example = "12", required = true)
            @PathVariable Long userId
    ) {
        return teamService.removeMemberFromTeam(teamId, userId);
    }

    // Everyone can view team members
    @GetMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN','PLAYER')")
    @Operation(
            summary = "Get team members",
            description = "Returns all members assigned to a team. Requires the ADMIN, CAPTAIN, or PLAYER role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Team members retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = TeamMemberResponse.class))
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Team not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Required role not granted")
    })
    public List<TeamMemberResponse> getTeamMembers(
            @Parameter(description = "Unique team ID", example = "1", required = true)
            @PathVariable Long teamId
    ) {
        return teamService.getTeamMembers(teamId);
    }

    // Admin only - edit team info like name/description
    @PutMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a team",
            description = "Updates team details and captain assignment. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Team updated successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "Team updated successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request, team not found, or captain not found"
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public String updateTeam(
                             @Parameter(description = "Unique team ID", example = "1", required = true)
                             @PathVariable Long teamId,
                             @Valid @RequestBody TeamRequest request) {
        return teamService.updateTeam(teamId, request);
    }

    // Admin only - delete team
    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Delete a team",
            description = "Deletes an unused team with no members. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Team deleted successfully",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(type = "string", example = "Team deleted successfully")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Team not found, still has members, or is used in matches"
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public String deleteTeam(
            @Parameter(description = "Unique team ID", example = "1", required = true)
            @PathVariable Long teamId
    ) {
        return teamService.deleteTeam(teamId);
    }

    // Captain/Admin can see available members to add into team
    @GetMapping("/{teamId}/available-members")
    @PreAuthorize("hasAnyRole('ADMIN','CAPTAIN')")
    @Operation(
            summary = "Get available team members",
            description = "Returns approved users who are not already members of the team. Requires the ADMIN or CAPTAIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Available members retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(
                                    schema = @Schema(implementation = AvailableTeamMemberResponse.class)
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Team not found"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "ADMIN or CAPTAIN role required")
    })
    public List<AvailableTeamMemberResponse> getAvailableMembers(
            @Parameter(description = "Unique team ID", example = "1", required = true)
            @PathVariable Long teamId
    ) {
        return teamService.getAvailableMembers(teamId);
    }
}
