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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.hsc.error.HSCError;
import it.smartcommunitylab.playandgo.hsc.service.PlayerTeamService;

/**
 * @author raman
 *
 */
@RestController
public class AdminController {

	@Autowired
	private PlayerTeamService teamService;
	
	@PostMapping("/api/admin/initiatives/{initiativeId}/player/subscribe")
	public ResponseEntity<String> subscribeTeamMember(@PathVariable String initiativeId, 
			@RequestParam String nickname, @RequestParam String teamId) throws HSCError {
		return ResponseEntity.ok(teamService.subscribeTeamMember(initiativeId, nickname, teamId));
	}
	
	@PostMapping("/api/admin/initiatives/{initiativeId}/player/unsubscribe")
	public ResponseEntity<Void> unsubscribeTeamMember(@PathVariable String initiativeId, @RequestParam String nickname) throws HSCError {
		//TODO unsubscribeTeamMember
		return new ResponseEntity<>(null, HttpStatus.OK);
	}
	
	@PostMapping("/api/admin/initiatives/{initiativeId}/player/unregister")
	public ResponseEntity<Void> unregisterPlayer(@PathVariable String initiativeId, @RequestParam String playerId, @RequestParam String nickname) throws HSCError {
		teamService.unregisterPlayer(initiativeId, playerId, nickname);
		return ResponseEntity.ok(null);
	}

}
