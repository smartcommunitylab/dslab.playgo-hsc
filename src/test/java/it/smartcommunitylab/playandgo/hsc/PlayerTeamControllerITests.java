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

package it.smartcommunitylab.playandgo.hsc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.hsc.domain.Campaign;
import it.smartcommunitylab.playandgo.hsc.domain.CampaignSubscription;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;
import it.smartcommunitylab.playandgo.hsc.domain.UserRole;
import it.smartcommunitylab.playandgo.hsc.domain.UserRole.Role;
import it.smartcommunitylab.playandgo.hsc.repository.InitiativeRepository;
import it.smartcommunitylab.playandgo.hsc.repository.PlayerTeamRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * @author raman
 *
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = {HSCApplication.class, TestingConifg.class})
public class PlayerTeamControllerITests {

	private static final String TEST_OWNER = "owner";
	private static final String TEST_NICKNAME = "nickname1";
	private static final String TEST_TERRITORY = "territory1";
	private static final String TEST_PLAYER_ID = "playerId1";
	private static final String TEST_CAMPAIGN = "campaign1";
	private static final String TEST_PLAYER_ID2 = "playerId2";
	private static final String TEST_NICKNAME2 = "nickname2";
	
	@Autowired
	private MockWebServer mockServer;
    @Autowired
    private MockMvc restMockMvc;
    @Autowired
    private InitiativeRepository initiativeRepo;
    @Autowired
    private PlayerTeamRepository teamRepo;
    @MockBean
    private JwtDecoder jwtDecoder;
    
	private ObjectMapper mapper = new ObjectMapper();	
	
	@BeforeEach
	public void setUp() {
		initiativeRepo.deleteAll();
		teamRepo.deleteAll();
	}
	
	@Test
//	@WithMockUser(username = "admin")
	public void testCampaigns() throws Exception {
		// should read initiatives locally
		prepareAdminRoles();
        restMockMvc.perform(
                get("/api/initiatives")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value("0"));
        
        
        // force sync, store in DB, read from DB
		prepareAdminRoles();
		prepareDefaultCampaigns();
		prepareAdminRoles();
		restMockMvc.perform(
                put("/api/initiatives")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value("1"));

		restMockMvc.perform(
                get("/api/initiatives/"+TEST_CAMPAIGN+"/model")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiativeId").value(TEST_CAMPAIGN));

		prepareAdminRoles();
		restMockMvc.perform(
                get("/api/initiatives/"+TEST_CAMPAIGN+"/admin")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("true"));

	}
	
	@Test
	public void testTeam() throws Exception {
        // create initiative
		prepareAdminRoles();
		prepareDefaultCampaigns();
		prepareAdminRoles();
		restMockMvc.perform(
                put("/api/initiatives")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
		
		prepareAdminRoles();
		prepareSubscription();
		PlayerTeam team = defaultTeam();
		restMockMvc.perform(
                post("/api/initiatives/" + TEST_CAMPAIGN+ "/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(team))
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
		
		String teamId = teamRepo.findAll().get(0).getId();
		prepareAdminRoles();
		team.setId(teamId);
		restMockMvc.perform(
                post("/api/initiatives/" + TEST_CAMPAIGN+ "/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(team))
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());

		prepareAdminRoles();
		restMockMvc.perform(
                get("/api/initiatives/"+TEST_CAMPAIGN+"/teams")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(teamId));

		preparePositions();
		restMockMvc.perform(
                get("/publicapi/"+TEST_CAMPAIGN+"/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value("1"))
                .andExpect(jsonPath("$[0].score").value("100.0"))
                .andExpect(jsonPath("$[0].position").value("1"))
                ;
		
		
		prepareAdminRoles();
		prepareUnsubscription();
		restMockMvc.perform(
                delete("/api/initiatives/" + TEST_CAMPAIGN+ "/team/" + teamId)
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());

	}

	
	/**
	 * @throws JsonProcessingException 
	 * 
	 */
	private void preparePositions() throws JsonProcessingException {
		Map<String, Double> scores = new HashMap<>();
		scores.put(TEST_NICKNAME, 100d);
		mockServer.enqueue(new MockResponse()
			      .setBody(mapper.writeValueAsString(scores))
			      .addHeader("Content-Type", "application/json"));
	}

	@Test
	public void testMembers() throws Exception {
        // create initiative
		prepareAdminRoles();
		prepareDefaultCampaigns();
		prepareAdminRoles();
		restMockMvc.perform(
                put("/api/initiatives")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
		
		prepareAdminRoles();
		prepareSubscription();
		PlayerTeam team = defaultTeam();
		restMockMvc.perform(
                post("/api/initiatives/" + TEST_CAMPAIGN+ "/team")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(team))
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());
		
		preparePlayers();
		restMockMvc.perform(
                get("/api/initiatives/" + TEST_CAMPAIGN+ "/team/candidates")
                .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value("2"))
                .andExpect(jsonPath("$.content[0].subscribed").value("true"))
                .andExpect(jsonPath("$.content[1].subscribed").value("false"))
                ;


	}
	
	
	/**
	 * @throws JsonProcessingException 
	 * 
	 */
	private void preparePlayers() throws JsonProcessingException {
		PlayerInfo p = new PlayerInfo();
		p.setPlayerId(TEST_PLAYER_ID);
		p.setNickname(TEST_NICKNAME);;
		List<PlayerInfo> list = new LinkedList<>();
		list.add(p);
		p = new PlayerInfo();
		p.setPlayerId(TEST_PLAYER_ID2);
		p.setNickname(TEST_NICKNAME2);;
		list.add(p);
		PageImpl<PlayerInfo> page = new PageImpl<>(list);
		mockServer.enqueue(new MockResponse()
			      .setBody(mapper.writeValueAsString(page))
			      .addHeader("Content-Type", "application/json"));
	}

	private PlayerTeam defaultTeam() {
		PlayerTeam team = new PlayerTeam();
		team.setInitiativeId(TEST_CAMPAIGN);
		team.setExpected(20);
		team.setOwner(TEST_OWNER);
		team.setMembers(Collections.singleton(TEST_NICKNAME));
		team.setCustomData(new HashMap<>());
		return team;
	}

	private void prepareSubscription() throws JsonProcessingException {
		CampaignSubscription cs = new CampaignSubscription();
		cs.setCampaignId(TEST_CAMPAIGN);
		cs.setId("111");
		cs.setPlayerId(TEST_PLAYER_ID);
		cs.setRegistrationDate(LocalDate.now().toString());
		cs.setTerritoryId(TEST_TERRITORY);

		mockServer.enqueue(new MockResponse()
			      .setBody(mapper.writeValueAsString(cs))
			      .addHeader("Content-Type", "application/json"));

	}
	private void prepareUnsubscription() throws JsonProcessingException {
		CampaignSubscription cs = new CampaignSubscription();
		cs.setCampaignId(TEST_CAMPAIGN);
		cs.setId("111");
		cs.setPlayerId(TEST_PLAYER_ID);
		cs.setRegistrationDate(LocalDate.now().toString());
		cs.setTerritoryId(TEST_TERRITORY);

		mockServer.enqueue(new MockResponse()
			      .setBody(mapper.writeValueAsString(cs))
			      .addHeader("Content-Type", "application/json"));

	}

	private void prepareDefaultCampaigns() throws JsonProcessingException {
		mockServer.enqueue(new MockResponse()
			      .setBody(mapper.writeValueAsString(Collections.singletonList(defaultCampaign())))
			      .addHeader("Content-Type", "application/json"));
	}

	private void prepareAdminRoles() throws JsonProcessingException {
		mockServer.enqueue(new MockResponse()
			      .setBody(mapper.writeValueAsString(adminRoles()))
			      .addHeader("Content-Type", "application/json"));
	}

	/**
	 * @return
	 */
	private List<UserRole> adminRoles() {
		UserRole r = new UserRole();
		r.setRole(Role.admin);
		return Collections.singletonList(r);
	}

	/**
	 * @return
	 */
	private Campaign defaultCampaign() {
		Campaign campaign = new Campaign();
		campaign.setCampaignId(TEST_CAMPAIGN);
		campaign.setDateFrom(LocalDate.now().minusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		campaign.setDateTo(LocalDate.now().plusDays(30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
		return campaign;
	}

}
