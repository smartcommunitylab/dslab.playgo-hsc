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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.playandgo.hsc.dto.CampaignPlacing;
import it.smartcommunitylab.playandgo.hsc.dto.GameStats;
import it.smartcommunitylab.playandgo.hsc.dto.TransportStat;
import it.smartcommunitylab.playandgo.hsc.repository.InitiativeRepository;
import it.smartcommunitylab.playandgo.hsc.repository.PlayerTeamRepository;
import it.smartcommunitylab.playandgo.hsc.security.SecurityHelper;

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
	private InitiativeRepository initiativeRepo;
	@Autowired
	private PlayerTeamRepository teamRepo;
	@Autowired
	private SecurityHelper securityHelper;

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
            String dateFrom, String dateTo) {
    	return engineService.getGroupTransportStatsGroupByMean(groupId, campaignId, metric, dateFrom, dateTo);
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


}
