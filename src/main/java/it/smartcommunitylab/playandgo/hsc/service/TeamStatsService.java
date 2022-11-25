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

import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;
import it.smartcommunitylab.playandgo.hsc.dto.CampaignPlacing;
import it.smartcommunitylab.playandgo.hsc.dto.GameStats;
import it.smartcommunitylab.playandgo.hsc.dto.PlacingComparison;
import it.smartcommunitylab.playandgo.hsc.dto.TransportStat;
import it.smartcommunitylab.playandgo.hsc.repository.PlayerTeamRepository;

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

	@PostConstruct
	private void init() {			
	}
	
	public Page<CampaignPlacing> getCampaignPlacing(String campaignId, String metric, String mean,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		return engineService.getCampaignPlacing(campaignId, metric, mean, dateFrom, dateTo, pageRequest);
	}
	
    public CampaignPlacing getCampaignPlacingByGroup(String groupId, String campaignId, 
            String metric, String mean, String dateFrom, String dateTo) {
    	return engineService.getCampaignPlacingByGroup(groupId, campaignId, metric, mean, dateFrom, dateTo);
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
		return engineService.getCampaignPlacingByGame(campaignId, dateFrom, dateTo, pageRequest);
	}
    
    public CampaignPlacing getCampaignPlacingByGameAndGroup(String groupId, String campaignId,
            String dateFrom, String dateTo) {
    	return engineService.getCampaignPlacingByGameAndGroup(groupId, campaignId, dateFrom, dateTo);
    }

    public PlacingComparison getCampaignPlacingByGameComparison(String groupId, String campaignId,
            String dateFrom, String dateTo) {
    	PlacingComparison result = new PlacingComparison(); 
    	//get campaign placing
    	List<CampaignPlacing> list = getCampaignPlacingByGame(campaignId, dateFrom, dateTo, PageRequest.of(0, 1000)).getContent();
    	//find my placing
    	int myPos = -1;
    	for(CampaignPlacing cp : list) {
    		if(cp.getGroupId().equals(groupId)) {
    			result.setMyPlacing(cp);
    			myPos = cp.getPosition();
    		}
    	}
    	if(myPos == -1) {
    		//last position
    		myPos = list.size() + 1;
    		CampaignPlacing myCp = new CampaignPlacing();
    		myCp.setGroupId(groupId);
    		myCp.setPosition(myPos);
    		myCp.setValue(0.0);
    		result.setMyPlacing(myCp);
    	}
    	int prevPos = myPos - 1;
    	if(prevPos > 0) {
    		result.setPrevPlacing(list.get(prevPos - 1));
    	}
    	int nextPos = myPos + 1;
    	if(nextPos <= list.size()) {
    		result.setNextPlacing(list.get(nextPos - 1));
    	}
    	return result;
    }
}
