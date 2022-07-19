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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeamStats;
import it.smartcommunitylab.playandgo.hsc.error.NotFoundException;
import it.smartcommunitylab.playandgo.hsc.error.OperationNotPermittedException;
import it.smartcommunitylab.playandgo.hsc.service.PlayerTeamService;

/**
 * @author raman
 *
 */
@RestController
public class PlayerController {

	@Autowired
	private PlayerTeamService teamService;
	

	@PostMapping("/publicapi/{initiativeId}/teams/{teamId}/members")
	public 
	ResponseEntity<PlayerInfo> subscribeCampaign(@PathVariable String initiativeId, @PathVariable String teamId) throws OperationNotPermittedException, NotFoundException {
		return ResponseEntity.ok(teamService.subscribeCampaign(initiativeId, teamId));
	}
	@DeleteMapping("/publicapi/{initiativeId}/teams/{teamId}/members")
	public 
	ResponseEntity<Void> unsubscribeCampaign(@PathVariable String initiativeId, @PathVariable String teamId) throws OperationNotPermittedException, NotFoundException {
		teamService.unsubscribeCampaign(initiativeId, teamId);
		return ResponseEntity.ok(null);
	}
	
//	@GetMapping("/publicapi/{initiativeId}/teams/{teamId}/members/me")
//	public
//	ResponseEntity<PlayerTeamStats> getStats(
//			@PathVariable String initiativeId,
//			@PathVariable String teamId,
//			@RequestParam String groupMode,
//			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateFrom,
//			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateTo) throws NotFoundException {
//		return ResponseEntity.ok(teamService.getPlayerTeamStats(initiativeId, teamId, groupMode, dateFrom, dateTo));
//	} 

}
