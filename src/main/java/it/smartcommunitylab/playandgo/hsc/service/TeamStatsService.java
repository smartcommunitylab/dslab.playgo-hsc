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

package it.smartcommunitylab.playandgo.hsc.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.playandgo.hsc.domain.Image;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;
import it.smartcommunitylab.playandgo.hsc.dto.CampaignPlacing;
import it.smartcommunitylab.playandgo.hsc.dto.GameStats;
import it.smartcommunitylab.playandgo.hsc.dto.PlacingComparison;
import it.smartcommunitylab.playandgo.hsc.dto.TransportStat;
import it.smartcommunitylab.playandgo.hsc.repository.PlayerTeamRepository;
import it.smartcommunitylab.playandgo.hsc.service.PlayGoEngineClientService.RestPage;

/**
 * @author nori
 *
 */
@Service
public class TeamStatsService {
	
	private static final Logger logger = LoggerFactory.getLogger(TeamStatsService.class);

	@Autowired
	private PlayGoEngineClientService engineService;
	
	@Autowired
	private PlayerTeamRepository teamRepo;
	
	@Autowired
	private AvatarService avatarService;

	@PostConstruct
	private void init() {			
	}
	
	public Page<CampaignPlacing> getCampaignPlacing(String campaignId, String metric, String mean,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		RestPage<CampaignPlacing> page = engineService.getCampaignPlacing(campaignId, metric, mean, dateFrom, dateTo, pageRequest);
		page.getContent().forEach(c -> updatePlacing(c, null));
		return page;
	}
	
    public CampaignPlacing getCampaignPlacingByGroup(String groupId, String campaignId, 
            String metric, String mean, String dateFrom, String dateTo) {
    	CampaignPlacing placing = engineService.getCampaignPlacingByGroup(groupId, campaignId, metric, mean, dateFrom, dateTo);
    	updatePlacing(placing, groupId);
    	return placing;
    }
    
    public List<TransportStat> getGroupTransportStats(String groupId, String campaignId, String groupMode, String metric, 
            String mean, String dateFrom, String dateTo) {
    	return engineService.getGroupTransportStats(groupId, campaignId, groupMode, metric, mean, dateFrom, dateTo);
    }
    
    public List<TransportStat> getGroupTransportStatsGroupByMean(String groupId, String campaignId, String metric,
            String dateFrom, String dateTo, boolean avg) {
    	List<TransportStat> list = engineService.getGroupTransportStatsGroupByMean(groupId, campaignId, metric, dateFrom, dateTo);
    	if(avg) {
    		PlayerTeam team = teamRepo.findById(groupId).orElse(null);
    		if(team != null) {
    			int size = team.getMembers().size();
    			for(TransportStat ts : list) {
    				ts.setValue(ts.getValue() / size);
    			}
    		}
    	}
    	return list;
    }
    
    public List<GameStats> getGroupGameStats(String groupId, String campaignId, String groupMode, 
            String dateFrom, String dateTo) {
    	return engineService.getGroupGameStats(groupId, campaignId, groupMode, dateFrom, dateTo);
    }
    
	public Page<CampaignPlacing> getCampaignPlacingByGame(String campaignId,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		RestPage<CampaignPlacing> page = engineService.getCampaignPlacingByGame(campaignId, dateFrom, dateTo, true, pageRequest);
		page.getContent().forEach(c -> updatePlacing(c, null));
		return page;
	}
    
    public CampaignPlacing getCampaignPlacingByGameAndGroup(String groupId, String campaignId,
            String dateFrom, String dateTo) {
    	CampaignPlacing placing = engineService.getCampaignPlacingByGameAndGroup(groupId, campaignId, dateFrom, dateTo);
    	updatePlacing(placing, groupId);
    	return placing;    	
    }

    public PlacingComparison getCampaignPlacingByGameGroupComparison(String groupId, String campaignId,
            String dateFrom, String dateTo) {
    	PlacingComparison result = new PlacingComparison();
    	result.setMin(Double.MAX_VALUE);
    	//get campaign placing
    	List<CampaignPlacing> list = getCampaignPlacingByGame(campaignId, dateFrom, dateTo, PageRequest.of(0, 1000)).getContent();
    	for(CampaignPlacing cp : list) {
    		if(cp.getGroupId().equals(groupId)) {
    			result.setValue(cp.getValue());
    		}
    		if(cp.getValue() > result.getMax()) {
    			result.setMax(cp.getValue());
    		}
    		if(cp.getValue() < result.getMin()) {
    			result.setMin(cp.getValue());
    		}
    	}
    	return result;
    }
    
	public PlacingComparison getCampaignPlacingByGamePlayerComparison(String playerId, String campaignId,
			String dateFrom, String dateTo) {
    	PlacingComparison result = new PlacingComparison();
    	result.setMin(Double.MAX_VALUE);
    	//get campaign placing
    	List<CampaignPlacing> list = engineService.getCampaignPlacingByGame(campaignId, dateFrom, dateTo, false, 
    			PageRequest.of(0, 5000)).getContent();
    	for(CampaignPlacing cp : list) {
    		if(cp.getPlayerId().equals(playerId)) {
    			result.setValue(cp.getValue());
    		}
    		if(cp.getValue() > result.getMax()) {
    			result.setMax(cp.getValue());
    		}
    		if(cp.getValue() < result.getMin()) {
    			result.setMin(cp.getValue());
    		}
    	}
    	return result;
	}
	
    private void updatePlacing(CampaignPlacing c, String groupId) {
		Image avatar = avatarService.getTeamSmallAvatar(c.getGroupId());
		if(avatar != null) {
			c.setAvatar(avatar);
		}
		if(groupId == null) {
			groupId = c.getGroupId();
		}
    	PlayerTeam team = teamRepo.findById(groupId).orElse(null);
    	if(team != null) {
    		if(team.getCustomData().containsKey(PlayerTeamService.KEY_NAME)) {
    			c.getCustomData().put(PlayerTeamService.KEY_NAME, team.getCustomData().get(PlayerTeamService.KEY_NAME));
    		}
    		if(team.getCustomData().containsKey(PlayerTeamService.KEY_INSTITUTE)) {
    			c.getCustomData().put(PlayerTeamService.KEY_INSTITUTE, team.getCustomData().get(PlayerTeamService.KEY_INSTITUTE));
    		}
    		if(team.getCustomData().containsKey(PlayerTeamService.KEY_SCHOOL)) {
    			c.getCustomData().put(PlayerTeamService.KEY_SCHOOL, team.getCustomData().get(PlayerTeamService.KEY_SCHOOL));
    		}
    		if(team.getCustomData().containsKey(PlayerTeamService.KEY_CLASS)) {
    			c.getCustomData().put(PlayerTeamService.KEY_CLASS, team.getCustomData().get(PlayerTeamService.KEY_CLASS));
    		}
    	}
    }

    
}
