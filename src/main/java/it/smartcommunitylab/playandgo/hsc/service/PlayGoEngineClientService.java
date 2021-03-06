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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.smartcommunitylab.playandgo.hsc.domain.Campaign;
import it.smartcommunitylab.playandgo.hsc.domain.CampaignSubscription;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.UserRole;
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
	public Map<String, Double> getPositions(String campaignId, Collection<String> nickNames) {
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
