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

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiParam;
import it.smartcommunitylab.playandgo.hsc.dto.CampaignPlacing;
import it.smartcommunitylab.playandgo.hsc.dto.GameStats;
import it.smartcommunitylab.playandgo.hsc.dto.TransportStat;
import it.smartcommunitylab.playandgo.hsc.error.HSCError;
import it.smartcommunitylab.playandgo.hsc.service.TeamStatsService;

/**
 * @author nori
 *
 */
@RestController
public class TeamStatsController {

	@Autowired
	TeamStatsService teamStatsService;

	@GetMapping("/api/data/campaign/placing/transport")
	public Page<CampaignPlacing> getCampaingPlacingByTransportStats(
			@RequestParam String campaignId,
			@RequestParam String metric,
			@RequestParam(required = false) String mean,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
			Pageable pageRequest) throws HSCError {
		return teamStatsService.getCampaignPlacing(campaignId, metric, mean, dateFrom, dateTo, pageRequest);			
	}

    @GetMapping("/api/data/campaign/placing/group/transport")
    public CampaignPlacing geGroupCampaingPlacingByTransportMode(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String metric,
            @RequestParam(required = false) String mean,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws HSCError {
    	return teamStatsService.getCampaignPlacingByGroup(groupId, campaignId, metric, mean, dateFrom, dateTo);
    }

    @GetMapping("/api/data/campaign/transport/group/stats")
    public List<TransportStat> getGroupTransportStats(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String metric,
            @RequestParam(required = false) String groupMode,
            @RequestParam(required = false) String mean,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws HSCError {
    	return teamStatsService.getGroupTransportStats(groupId, campaignId, groupMode, metric, mean, dateFrom, dateTo);
    }
    
    @GetMapping("/api/data/campaign/transport/group/stats/mean")
    public List<TransportStat> getGroupTransportStatsGroupByMean(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam String metric,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws Exception {
    	return teamStatsService.getGroupTransportStatsGroupByMean(groupId, campaignId, metric, dateFrom, dateTo);
    }
    
	@GetMapping("/api/data/campaign/group/game/stats")
	public List<GameStats> getGroupGameStats(
			@RequestParam String campaignId,
			@RequestParam String groupId,
			@RequestParam String groupMode,
			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateFrom,
			@RequestParam @ApiParam(value = "yyyy-MM-dd") String dateTo,
			HttpServletRequest request) throws Exception {
		return teamStatsService.getGroupGameStats(groupId, campaignId, groupMode, dateFrom, dateTo);
	}
	
    @GetMapping("/api/data/campaign/placing/game")
    public Page<CampaignPlacing> getCampaingPlacingByGame(
            @RequestParam String campaignId,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
            Pageable pageRequest,
            HttpServletRequest request) throws Exception {
    	return teamStatsService.getCampaignPlacingByGame(campaignId, dateFrom, dateTo, pageRequest);            
    }
    
    @GetMapping("/api/data/campaign/placing/group/game")
    public CampaignPlacing getGroupCampaingPlacingByGame(
            @RequestParam String campaignId,
            @RequestParam String groupId,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateFrom,
            @RequestParam(required = false) @ApiParam(value = "yyyy-MM-dd") String dateTo,
            HttpServletRequest request) throws Exception {
        return teamStatsService.getCampaignPlacingByGameAndGroup(groupId, campaignId, dateFrom, dateTo);
    }

}