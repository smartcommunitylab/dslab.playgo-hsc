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

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.playandgo.hsc.domain.Campaign;
import it.smartcommunitylab.playandgo.hsc.domain.CampaignGroupPlacing;
import it.smartcommunitylab.playandgo.hsc.domain.CampaignSubscription;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.TeamMember;
import it.smartcommunitylab.playandgo.hsc.domain.UserRole;
import it.smartcommunitylab.playandgo.hsc.dto.CampaignPlacing;
import it.smartcommunitylab.playandgo.hsc.dto.GameStats;
import it.smartcommunitylab.playandgo.hsc.dto.TransportStat;
import it.smartcommunitylab.playandgo.hsc.security.SecurityHelper;

/**
 * @author raman
 *
 */
@Service
public class PlayGoEngineClientService {

	@Autowired
	private WebClient webClient;
	
	@Autowired
	private SecurityHelper securityHelper;
	
	@SuppressWarnings("unchecked")
	public Map<String, Double> getPositions(String campaignId, Collection<TeamMember> teamMembers) {
		Set<String> nickNames = teamMembers.stream().map(tm -> tm.getNickname()).collect(Collectors.toSet());
		return 
		webClient.post()
		.uri("/api/ext/campaign/game/placing?campaignId="+campaignId)
		.contentType(MediaType.APPLICATION_JSON)
		.bodyValue(nickNames)
		.attributes(clientRegistrationId("oauthprovider"))
		.retrieve()
		.bodyToMono(Map.class)
		.block();
	}

	@SuppressWarnings("unchecked")
	public List<CampaignGroupPlacing> getPositions(String campaignId, String dateFrom, String dateTo) {
		ParameterizedTypeReference<List<CampaignGroupPlacing>> ref = new ParameterizedTypeReference<List<CampaignGroupPlacing>>() {};
		return 
		webClient.get()
		.uri("/api/ext/campaign/game/group/placing?campaignId="+campaignId+"&dateFrom="+dateFrom+"&dateTo="+dateTo)
		.attributes(clientRegistrationId("oauthprovider"))
		.retrieve()
		.bodyToMono(ref)
		.block();
	}
	
	public List<UserRole> getUserRoles() {
		ParameterizedTypeReference<List<UserRole>> ref = new ParameterizedTypeReference<List<UserRole>>() {};
		return 
		webClient.get()
		.uri("/api/console/role/my")
		.header("Authorization", "Bearer " + securityHelper.getCurrentToken())
		.retrieve()
		.bodyToMono(ref)
		.block();
	}

	public List<PlayerInfo> getPlayersWithAvatars(String territory, List<String> players) {
		ParameterizedTypeReference<List<PlayerInfo>> ref = new ParameterizedTypeReference<List<PlayerInfo>>() {};
		String uri = "/api/ext/territory/players/avatar?territory="+territory;
		for(String playerId : players) {
			uri = uri + "&players=" + playerId;
		}
		return 
		webClient.get()
		.uri(uri)
		.header("Authorization", "Bearer " + securityHelper.getCurrentToken())
		.retrieve()
		.bodyToMono(ref)
		.block();
	}
	
	public RestPage<PlayerInfo> getPlayers(String txt, String territory, Pageable pageRequest) {
		ParameterizedTypeReference<RestPage<PlayerInfo>> ref = new ParameterizedTypeReference<RestPage<PlayerInfo>>() {};
		return 
		webClient.get()
		.uri("/api/ext/territory/players?territory="+territory + "&txt="+(txt == null ? "" : txt.trim()))
		.header("Authorization", "Bearer " + securityHelper.getCurrentToken())
		.retrieve()
		.bodyToMono(ref)
		.block();
	}
	public PlayerInfo getPlayer(String playerId, String territory) {
		return 
		webClient.get()
		.uri("/api/ext/territory/players/"+playerId+"?territory="+territory)
		.header("Authorization", "Bearer " + securityHelper.getCurrentToken())
		.retrieve()
		.bodyToMono(PlayerInfo.class)
		.block();
	}
	
	public List<Campaign> getCampaigns() {
		ParameterizedTypeReference<List<Campaign>> ref = new ParameterizedTypeReference<List<Campaign>>() {};
		return 
		webClient.get()
		.uri("/publicapi/campaign?type=school")
		.attributes(clientRegistrationId("oauthprovider"))
		.retrieve()
		.bodyToMono(ref)
		.block();
	}


	/**
	 * @param initiativeId
	 * @param teamId
	 * @param groupMode
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public List<GameStats> getTeamStats(String initiativeId, String teamId, String groupMode, String dateFrom,
			String dateTo) {
		ParameterizedTypeReference<List<GameStats>> ref = new ParameterizedTypeReference<List<GameStats>>() {};
		return 
		webClient.get()
		.uri("/api/ext/campaign/game/group/stats?campaignId="+initiativeId+"&groupId="+teamId+"&groupMode="+groupMode+"&dateFrom="+dateFrom+"&dateTo="+dateTo)
		.attributes(clientRegistrationId("oauthprovider"))
		.retrieve()
		.bodyToMono(ref)
		.block();	
	}
	public CampaignSubscription subscribe(String campaignId, String nickName, Map<String, Object> campaignData) {
		return webClient.post()
		.uri("/api/ext/campaign/subscribe/territory?campaignId="+campaignId+"&nickname="+nickName)
		.contentType(MediaType.APPLICATION_JSON)
		.bodyValue(campaignData == null ? Collections.emptyMap() : campaignData)
		.attributes(clientRegistrationId("oauthprovider"))
		.retrieve()
		.bodyToMono(CampaignSubscription.class)
		.block();
	}
	public CampaignSubscription unsubscribe(String campaignId, String nickName) {
		return webClient.delete()
		.uri("/api/ext/campaign/unsubscribe/territory?campaignId="+campaignId+"&nickname="+nickName)
		.attributes(clientRegistrationId("oauthprovider"))
		.retrieve()
		.bodyToMono(CampaignSubscription.class)
		.block();
	}
	
	public RestPage<CampaignPlacing> getCampaignPlacing(String campaignId, String metric, String mean,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		String uri = "/api/report/campaign/placing/transport?campaignId=" + campaignId + "&metric=" + metric + "&groupByGroupId=true";
		if(StringUtils.hasText(mean)) {
			uri = uri + "&mean=" + mean;
		}
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		uri = uri + "&size=" + pageRequest.getPageSize() + "&page=" + pageRequest.getPageNumber();
		ParameterizedTypeReference<RestPage<CampaignPlacing>> ref = new ParameterizedTypeReference<RestPage<CampaignPlacing>>() {};
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(ref)
				.block();
	}
	
    public CampaignPlacing getCampaignPlacingByGroup(String groupId, String campaignId, 
            String metric, String mean, String dateFrom, String dateTo) {
		String uri = "/api/report/campaign/placing/group/transport?campaignId=" + campaignId 
				+ "&groupId=" + groupId + "&metric=" + metric;
		if(StringUtils.hasText(mean)) {
			uri = uri + "&mean=" + mean;
		}
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(CampaignPlacing.class)
				.block();    	
    }
    
    public List<TransportStat> getGroupTransportStats(String groupId, String campaignId, String groupMode, String metric, 
            String mean, String dateFrom, String dateTo) {
		String uri = "/api/report/group/transport/stats?campaignId=" + campaignId 
				+ "&groupId=" + groupId + "&metric=" + metric;
		if(StringUtils.hasText(groupMode)) {
			uri = uri + "&groupMode=" + groupMode;
		}
		if(StringUtils.hasText(mean)) {
			uri = uri + "&mean=" + mean;
		}
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		ParameterizedTypeReference<List<TransportStat>> ref = new ParameterizedTypeReference<List<TransportStat>>() {};
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(ref)
				.block();		
    }

    public List<TransportStat> getGroupTransportStatsGroupByMean(String groupId, String campaignId, String metric,
            String dateFrom, String dateTo) {
		String uri = "/api/report/group/transport/stats/mean?campaignId=" + campaignId 
				+ "&groupId=" + groupId + "&metric=" + metric;
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		ParameterizedTypeReference<List<TransportStat>> ref = new ParameterizedTypeReference<List<TransportStat>>() {};
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(ref)
				.block();		    	
    }
    
    public List<GameStats> getGroupGameStats(String groupId, String campaignId, String groupMode, 
            String dateFrom, String dateTo) {
		String uri = "/api/report/group/game/stats?campaignId=" + campaignId 
				+ "&groupId=" + groupId + "&groupMode=" + groupMode;
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		ParameterizedTypeReference<List<GameStats>> ref = new ParameterizedTypeReference<List<GameStats>>() {};
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(ref)
				.block();		    	    	
    }

	public RestPage<CampaignPlacing> getCampaignPlacingByGame(String campaignId,  
			String dateFrom, String dateTo, Pageable pageRequest) {
		String uri = "/api/report/campaign/placing/game?campaignId=" + campaignId + "&groupByGroupId=true";
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		uri = uri + "&size=" + pageRequest.getPageSize() + "&page=" + pageRequest.getPageNumber();
		ParameterizedTypeReference<RestPage<CampaignPlacing>> ref = new ParameterizedTypeReference<RestPage<CampaignPlacing>>() {};
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(ref)
				.block();
	}
	
    public CampaignPlacing getCampaignPlacingByGameAndGroup(String groupId, String campaignId,
            String dateFrom, String dateTo) {
		String uri = "/api/report/campaign/placing/group/game?campaignId=" + campaignId 
				+ "&groupId=" + groupId;
		if(StringUtils.hasText(dateFrom) && StringUtils.hasText(dateTo)) {
			uri = uri + "&dateFrom=" + dateFrom + "&dateTo=" + dateTo;
		}
		return webClient.get()
				.uri(uri)
				.attributes(clientRegistrationId("oauthprovider"))
				.retrieve()
				.bodyToMono(CampaignPlacing.class)
				.block();    	    	
    }

	@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
	public static class RestPage<T> extends PageImpl<T> {
		private static final long serialVersionUID = -2872613325721095097L;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	    public RestPage(@JsonProperty("content") List<T> content,
	                     @JsonProperty("number") int page,
	                     @JsonProperty("size") int size,
	                     @JsonProperty("totalElements") long total) {
	        super(content, PageRequest.of(page, size), total);
	    }

	    public RestPage(Page<T> page) {
	        super(page.getContent(), page.getPageable(), page.getTotalElements());
	    }

		public RestPage() {
			super(Collections.emptyList());
		}
	    
	}

}