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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.playandgo.hsc.domain.Campaign;
import it.smartcommunitylab.playandgo.hsc.domain.CampaignGroupPlacing;
import it.smartcommunitylab.playandgo.hsc.domain.Initiative;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeamStats;
import it.smartcommunitylab.playandgo.hsc.domain.UserRole;
import it.smartcommunitylab.playandgo.hsc.error.DataException;
import it.smartcommunitylab.playandgo.hsc.error.HSCError;
import it.smartcommunitylab.playandgo.hsc.error.NotFoundException;
import it.smartcommunitylab.playandgo.hsc.error.OperationNotEnabledException;
import it.smartcommunitylab.playandgo.hsc.error.OperationNotPermittedException;
import it.smartcommunitylab.playandgo.hsc.repository.InitiativeRepository;
import it.smartcommunitylab.playandgo.hsc.repository.PlayerTeamRepository;
import it.smartcommunitylab.playandgo.hsc.security.SecurityHelper;

/**
 * @author raman
 *
 */
@Service
public class PlayerTeamService {
	
	private static final Logger logger = LoggerFactory.getLogger(PlayerTeamService.class);

	@Autowired
	private PlayGoEngineClientService engineService;
	@Autowired
	private InitiativeRepository initiativeRepo;
	@Autowired
	private PlayerTeamRepository teamRepo;
	@Autowired
	private SecurityHelper securityHelper;

	private LoadingCache<String, TeamClassification> teamState;

	@PostConstruct
	private void init() {

		teamState = CacheBuilder.newBuilder().refreshAfterWrite(30, TimeUnit.MINUTES).build(new CacheLoader<String, TeamClassification>() {
			@Override
			public TeamClassification load(String id) {
				PlayerTeam team = teamRepo.findById(id).orElse(null);
				if (team == null) return null;

				TeamClassification res = new TeamClassification();
				res.setId(team.getId());
				res.setCustomData(team.getCustomData());
				try {
					Double score = computeTeamScore(team.getInitiativeId(), id);
					if (team.getMembers() != null && team.getMembers().size() > 0) score = score / team.getMembers().size();
					res.setScore(score);
				} catch (Exception e) {
					logger.error("Error computing team score: "+ e.getMessage(), e);
					res.setScore(0d);
				}
				return res;
			}
		});
			
	}
	public List<Initiative> getInitativesForManager() {
		List<UserRole> roles = engineService.getUserRoles();
		if (isAdmin(roles)) return initiativeRepo.findAll();
		List<String> territories = getTerritories(roles);
		if (territories != null) return initiativeRepo.findByTerritories(territories);
		List<String> campaigns = getCampaigns(roles);
		if (campaigns != null) return initiativeRepo.findByCampaignIds(territories);

		return Collections.emptyList();
	}

	public Page<PlayerInfo> searchPlayers(String initiativeId, String txt, Pageable pageRequest) {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative != null) {
			Page<PlayerInfo> page = engineService.getPlayers(txt, initiative.getCampaign().getTerritoryId(), pageRequest);
			if (page.hasContent()) {
				final Set<String> registered = teamRepo.findByInitiativeId(initiativeId)
				.stream()
				.map(t -> t.getMembers())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
				page.getContent().forEach(p -> p.setSubscribed(registered.contains(p.getNickname())));
			}
			return page;
		}
		return Page.empty();
	}
	
	public PlayerTeam saveTeam(@PathVariable String initiativeId, PlayerTeam team) throws HSCError {
		String currentUser = securityHelper.getCurrentPreferredUsername();
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}

		List<Initiative> userInitiatives = getInitativesForManager();
		boolean isManager = userInitiatives.stream().anyMatch(i -> i.getInitiativeId().equals(initiativeId));
		team.setInitiativeId(initiativeId);
		
		Set<String> toRemove = new HashSet<>();
		Set<String> toAdd = new HashSet<>(team.getMembers());
		
		if (team.getId() != null) {
			PlayerTeam existing = teamRepo.findById(team.getId()).orElse(null);
			if (existing == null) {
				throw new NotFoundException("NO_TEAM");
			}

			if (!isManager && !existing.getOwner().equals(currentUser)) {
				// throw access exception
				throw new OperationNotPermittedException("OWNER");
			} 
			
			if (!isManager && !Boolean.TRUE.equals(initiative.getCanEdit())) {
				throw new OperationNotEnabledException("EDIT");
			}
			toRemove = new HashSet<>(existing.getMembers());
			toRemove.removeAll(team.getMembers());
			toAdd.removeAll(existing.getMembers());
		} else {
			if (!isManager && !Boolean.TRUE.equals(initiative.getCanCreate())) {
				throw new OperationNotEnabledException("CREATE");
			}
			team.setOwner(currentUser);
		}
		validate(team, initiative);
		
		/* DISABLED: NO SUBSCRIPTION BY MANAGER, JUST CONFIGURATION SAVE
		try {
			// try to subscribe
			for (String nickName: toAdd) {
				Map<String, Object> data = new HashMap<>(team.getCustomData());
				data.put("teamId", team.getId());
				engineService.subscribe(initiative.getInitiativeId(), nickName, team.getCustomData());
			}
			team = teamRepo.save(team);
		} catch (Exception e) {
			logger.error("Failed to subscribe: " + e.getMessage(), e);
			// subscription / save failed: undo subscribe
			for (String nickName: toAdd) {
				try {
					engineService.unsubscribe(initiative.getInitiativeId(), nickName);
				} catch (Exception e1) {
					logger.error("Failed to clean subscription "+ e1.getMessage());
				}
			}
			throw e;
		}
		*/
		team = teamRepo.save(team);

		// remove not used
		for (String nickName: toRemove) {
			try {
				engineService.unsubscribe(initiative.getInitiativeId(), nickName);
			} catch (Exception e1) {
				logger.error("Failed to remove subscription", e1);
			}
		}
		return team;
	}
	
	public void deleteTeam(String initiativeId, String teamId) throws HSCError {
		String currentUser = securityHelper.getCurrentPreferredUsername();
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}

		List<Initiative> userInitiatives = getInitativesForManager();
		boolean isManager = userInitiatives.stream().anyMatch(i -> i.getInitiativeId().equals(initiativeId));
		
		Set<String> toRemove = new HashSet<>();
		
		PlayerTeam existing = teamRepo.findById(teamId).orElse(null);
		if (existing == null) {
			throw new NotFoundException("NO_TEAM");
		}

		if (!isManager && !existing.getOwner().equals(currentUser)) {
			// throw access exception
			throw new OperationNotPermittedException("OWNER");
		} 
			
		if (!isManager && !Boolean.TRUE.equals(initiative.getCanEdit())) {
			throw new OperationNotEnabledException("EDIT");
		}
		toRemove = new HashSet<>(existing.getMembers());
		// remove not used
		for (String nickName: toRemove) {
			try {
				engineService.unsubscribe(initiative.getInitiativeId(), nickName);
			} catch (Exception e1) {
				logger.error("Failed to remove subscription", e1);
			}
		}
		teamRepo.delete(existing);
	}
	
	/**
	 * @param team
	 * @param initiative
	 * @throws DataException 
	 */
	private void validate(PlayerTeam team, Initiative initiative) throws DataException {
		Set<String> registered = teamRepo.findByInitiativeId(initiative.getInitiativeId())
				.stream()
				.filter(t -> !t.getId().equals(team.getId()))
				.map(t -> t.getMembers())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		if (!Collections.disjoint(registered, team.getMembers())) {
			throw new DataException("PLAYER");
		}
	}

	public Initiative getInitiative(String initiativeId) {
		return initiativeRepo.findById(initiativeId).orElse(null);
	}
	
	public void setInitiativeCreate(String initiativeId, boolean value) {
		List<Initiative> initiatives = getInitativesForManager();
		initiatives.stream().filter(i -> i.getInitiativeId().equals(initiativeId)).forEach(i -> {
			i.setCanCreate(value);
			initiativeRepo.save(i);
		});
	}
	public void setInitiativeEdit(String initiativeId, boolean value) {
		List<Initiative> initiatives = getInitativesForManager();
		initiatives.stream().filter(i -> i.getInitiativeId().equals(initiativeId)).forEach(i -> {
			i.setCanEdit(value);
			initiativeRepo.save(i);
		});
	}
	


	/**
	 * @param initiativeId
	 * @param initiative
	 * @return
	 */
	public Initiative saveInitiative(String initiativeId, Initiative initiative) {
		Initiative old = initiativeRepo.findById(initiativeId).orElse(null);
		if (old != null) {
			old.setBonus(initiative.getBonus());
			old.setBonusThreshold(initiative.getBonusThreshold());
			old.setMinTeamSize(initiative.getMinTeamSize());
			return initiativeRepo.save(old);
		}
		return null;
	}
	public List<PlayerTeam> getUserTeamsForInitiative(String initiativeId) {
		List<Initiative> initiatives = getInitativesForManager();
		if (initiatives.stream().anyMatch(i -> i.getInitiativeId().equals(initiativeId))) {
			return teamRepo.findByInitiativeId(initiativeId); 
		} else {
			return teamRepo.findByInitiativeIdAndOwner(initiativeId, securityHelper.getCurrentPreferredUsername());
		}
	}
	
	public void forceSync() {
		if (isAdmin(engineService.getUserRoles())) {
			syncExternalCampaigns();
		}
	}

	public  List<PlayerTeam> getPublicTeams(String initiative) {
		List<PlayerTeam> teams = teamRepo.findByInitiativeId(initiative);
		teams.forEach(t -> {
			t.setOwner(null);
			t.setExpected(0);
			t.setMembers(null);
		});
		return teams;
	}
	
	public  List<TeamClassification> getLeaderboard(String initiative) {
		List<PlayerTeam> teams = teamRepo.findByInitiativeId(initiative);
		List<TeamClassification> list = teams.stream().map(t -> {
			try {
				return teamState.get(t.getId());
			} catch (ExecutionException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(s -> s != null).collect(Collectors.toList());
		list.sort((a,b) -> a.getScore() > b.getScore() ? -1 : a.getScore() < b.getScore() ? 1 : 0);
		long points = -1;
		for (int i = 0; i < list.size(); i++) {
			TeamClassification c = list.get(i);
			if (c.getScore() == null) c.setScore(0d);
			long score = Math.round(c.getScore());
			if (score != points) {
				c.setPosition(i+1);
			} else {
				c.setPosition(list.get(i-1).getPosition());
			}
			points = score;
		}
		
		return list;
	}
	

	/**
	 * @param initiativeId
	 * @param dateFrom
	 * @param dateTo
	 * @return
	 */
	public List<TeamClassification> getLeaderboard(String initiativeId, String dateFrom, String dateTo) {
		List<PlayerTeam> teams = teamRepo.findByInitiativeId(initiativeId);
		Map<String, CampaignGroupPlacing> map = engineService.getPositions(initiativeId, dateFrom, dateTo)
				.stream().collect(Collectors.toMap(p -> p.getGroupId(), p -> p));

		List<TeamClassification>  res = teams.stream().map(t -> {
			TeamClassification tc = new TeamClassification();
			tc.setId(t.getId());
			tc.setCustomData(t.getCustomData());
			CampaignGroupPlacing p = map.get(tc.getId());
			if (p != null) {
				tc.setPosition(p.getPosition());
				tc.setScore(p.getValue());
			} else {
				tc.setPosition(0);
				tc.setScore(0d);
			}
			return tc;
		}).collect(Collectors.toList());
		res.sort((a,b) -> a.getPosition() - b.getPosition());
		return res;
	}
	
	/**
	 * Subscribe to the team: check nickname in the list of the members and then subscribe to engine
	 * @param initiativeId
	 * @param teamId
	 * @param player
	 * @return
	 * @throws OperationNotPermittedException 
	 * @throws NotFoundException 
	 */
	public PlayerInfo subscribeCampaign(String initiativeId, String teamId) throws OperationNotPermittedException, NotFoundException {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}
		String subj = securityHelper.getCurrentSubject();
		PlayerInfo ext = engineService.getPlayer(subj, initiative.getCampaign().getTerritoryId());
		if (ext == null) {
			throw new OperationNotPermittedException("PLAYER");
		}
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if (team == null) {
			throw new NotFoundException("NO_TEAM");
		}
		if (!team.getMembers().contains(ext.getNickname())) {
			throw new OperationNotPermittedException("MEMBER");
		}
		Map<String, Object> data = new HashMap<>(team.getCustomData());
		data.put("teamId", team.getId());
		engineService.subscribe(initiativeId, ext.getNickname(), team.getCustomData());
		return ext;
	}
	
	/**
	 * Subscribe to the team: check nickname in the list of the members and then subscribe to engine
	 * @param initiativeId
	 * @param teamId
	 * @param player
	 * @return
	 * @throws OperationNotPermittedException 
	 * @throws NotFoundException 
	 */
	public void unsubscribeCampaign(String initiativeId, String teamId) throws OperationNotPermittedException, NotFoundException {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}
		String subj = securityHelper.getCurrentSubject();
		PlayerInfo ext = engineService.getPlayer(subj, initiative.getCampaign().getTerritoryId());
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if (team == null) {
			throw new NotFoundException("NO_TEAM");
		}
		if (!team.getMembers().contains(ext.getNickname())) {
			throw new OperationNotPermittedException("MEMBER");
		}
		Map<String, Object> data = new HashMap<>(team.getCustomData());
		data.put("teamId", team.getId());
		engineService.unsubscribe(initiativeId, ext.getNickname());
		team.getMembers().remove(ext.getNickname());
		teamRepo.save(team);
	}
	


	/**
	 * @param initiativeId
	 * @param teamId
	 * @param dateTo 
	 * @param dateFrom 
	 * @param groupMode 
	 * @return
	 * @throws NotFoundException 
	 */
	public PlayerTeamStats getPlayerTeamStats(String initiativeId, String teamId, String groupMode, String dateFrom, String dateTo) throws NotFoundException {
		PlayerTeamStats stats = new PlayerTeamStats();
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}
		String subj = securityHelper.getCurrentSubject();
		PlayerInfo ext = engineService.getPlayer(subj, initiative.getCampaign().getTerritoryId());
		
		return stats;
	}

	
	@Scheduled(fixedDelay=1000*60*60*24, initialDelay = 1000*60*60) 
	public void syncExternalCampaigns() {
		List<Campaign> campaigns = engineService.getCampaigns();
		for (Campaign c : campaigns) {
			Initiative initiative = initiativeRepo.findById(c.getCampaignId()).orElse(null);
			if (initiative == null) {
				initiative = new Initiative();
				initiative.setInitiativeId(c.getCampaignId());
				initiative.setCanCreate(true);
				initiative.setCanEdit(true);
				initiative.setBonus(300d);
				initiative.setBonusThreshold(90d);
				initiative.setMinTeamSize(10);
				initiative.setType("hsc");
			}
			initiative.setCampaign(c);
			initiativeRepo.save(initiative);
		}
	}
	
	private boolean isAdmin(List<UserRole> roles) {
		return roles != null && roles.stream().anyMatch(r -> UserRole.Role.admin.equals(r.getRole()));
	}
	
	private List<String> getTerritories(List<UserRole> roles) {
		return roles.stream().filter(r -> UserRole.Role.territory.equals(r.getRole())).map(r -> r.getEntityId()).collect(Collectors.toList());
	}
	private List<String> getCampaigns(List<UserRole> roles) {
		return roles.stream().filter(r -> UserRole.Role.campaign.equals(r.getRole())).map(r -> r.getEntityId()).collect(Collectors.toList());
	}
	
	private Double computeTeamScore(String initiativeId, String teamId) {
			Initiative obj = getInitiative(initiativeId);
			PlayerTeam team = teamRepo.findById(teamId).orElse(null);
			if (team != null) {
				double score = 0;
				if (team.getMembers() != null && team.getMembers().size() > 0) {
					if (obj.getBonusThreshold() != null && obj.getBonus() != null && team.getExpected() != null && team.getExpected() > 0) {
						if ((100.0 * team.getMembers().size() / team.getExpected()) >= obj.getBonusThreshold()) {
							score += obj.getBonus() * team.getMembers().size();
						}
					}
					Long now = System.currentTimeMillis();
					if ((obj.getCampaign().getDateTo() == null || now < obj.getCampaign().getDateTo()) && 
						(obj.getCampaign().getDateFrom() == null || now > obj.getCampaign().getDateFrom())) {
						Map<String, Double> positions = engineService.getPositions(initiativeId, team.getMembers());
						score += positions.values().stream().collect(Collectors.summingDouble(v -> v));
					}
				}
				return score;
			} 
			return 0d;
	}


	public static class TeamClassification {
		private String id;
		private Double score;
		private int position;
		private Map<String, Object> customData;
		public String getId() {
			return id;
		}
		public void setId(String id) {
			this.id = id;
		}
		public Double getScore() {
			return score;
		}
		public void setScore(Double score) {
			this.score = score;
		}
		public Map<String, Object> getCustomData() {
			return customData;
		}
		public void setCustomData(Map<String, Object> customData) {
			this.customData = customData;
		}
		public int getPosition() {
			return position;
		}
		public void setPosition(int position) {
			this.position = position;
		}
		
	}




}
