/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.hsc.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.hsc.domain.Initiative;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;
import it.smartcommunitylab.playandgo.hsc.error.HSCError;
import it.smartcommunitylab.playandgo.hsc.service.PlayerTeamService;
import it.smartcommunitylab.playandgo.hsc.service.PlayerTeamService.TeamClassification;

/**
 * @author raman
 *
 */
@RestController
public class PlayerTeamController {

	@Autowired
	private PlayerTeamService teamService;
	

	@GetMapping("/publicapi/{initiativeId}/board")
	public 
	ResponseEntity<List<TeamClassification>> webBoardAPI(@PathVariable String initiativeId) {
		return ResponseEntity.ok(teamService.getLeaderboard(initiativeId));
	}

	@GetMapping("/api/initiatives/{initiativeId}/teams")
	public ResponseEntity<List<PlayerTeam>> getMyTeams(@PathVariable String initiativeId) {
		return ResponseEntity.ok(teamService.getUserTeamsForInitiative(initiativeId));
	}
	
	@GetMapping("/api/initiatives/{initiativeId}/model")
	public ResponseEntity<Initiative> getInitative(@PathVariable String initiativeId) {
		return ResponseEntity.ok(teamService.getInitiative(initiativeId));
	}

	@GetMapping("/api/initiatives/{initiativeId}/admin")
	public ResponseEntity<Boolean> isAdmin(@PathVariable String initiativeId) {
		boolean isAdmin = teamService.getInitativesForManager().stream().anyMatch(i -> i.getInitiativeId().equals(initiativeId));
		return ResponseEntity.ok(isAdmin);
	}
	
	@PutMapping("/api/initiatives")
	public ResponseEntity<List<Initiative>> syncInitatives() {
		teamService.forceSync();
		return ResponseEntity.ok(teamService.getInitativesForManager());
	}
	
	@GetMapping("/api/initiatives")
	public ResponseEntity<List<Initiative>> initatives() {
		return ResponseEntity.ok(teamService.getInitativesForManager());
	}

	@PostMapping("/api/initiatives/{initiativeId}/team")
	public ResponseEntity<PlayerTeam> saveTeam(@PathVariable String initiativeId, @RequestBody PlayerTeam team) throws HSCError {
		return ResponseEntity.ok(teamService.saveTeam(initiativeId, team));
	}
	@PutMapping("/api/initiatives/{initiativeId}")
	public ResponseEntity<Initiative> saveInitiative(@PathVariable String initiativeId, @RequestBody Initiative initiative) throws HSCError {
		return ResponseEntity.ok(teamService.saveInitiative(initiativeId, initiative));
	}
	@PutMapping("/api/initiatives/{initiativeId}/create/{value}")
	public ResponseEntity<Initiative> setCreate(@PathVariable String initiativeId, @PathVariable Boolean value) throws HSCError {
		teamService.setInitiativeCreate(initiativeId, value);
		return ResponseEntity.ok(teamService.getInitiative(initiativeId));
	}
	@PutMapping("/api/initiatives/{initiativeId}/edit/{value}")
	public ResponseEntity<Initiative> setEdit(@PathVariable String initiativeId, @PathVariable Boolean value) throws HSCError {
		teamService.setInitiativeEdit(initiativeId, value);
		return ResponseEntity.ok(teamService.getInitiative(initiativeId));
	}
	@DeleteMapping("/api/initiatives/{initiativeId}/team/{teamId}")
	public ResponseEntity<Void> deleteTeam(@PathVariable String initiativeId, @PathVariable String teamId) throws HSCError {
		teamService.deleteTeam(initiativeId, teamId);
		return ResponseEntity.ok(null);
	}

	@GetMapping("/api/initiatives/{initiativeId}/team/candidates")
	public ResponseEntity<Page<PlayerInfo>> candidates(@PathVariable String initiativeId, @RequestParam(required = false) String txt, Pageable pageRequest) {
		return ResponseEntity.ok(teamService.searchPlayers(initiativeId, txt, pageRequest));
	}

}
