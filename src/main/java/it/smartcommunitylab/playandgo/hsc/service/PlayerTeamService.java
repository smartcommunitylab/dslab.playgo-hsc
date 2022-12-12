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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.playandgo.hsc.domain.Campaign;
import it.smartcommunitylab.playandgo.hsc.domain.Image;
import it.smartcommunitylab.playandgo.hsc.domain.Initiative;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerInfo;
import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;
import it.smartcommunitylab.playandgo.hsc.domain.TeamMember;
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
	
	public static final String KEY_NAME = "name";
	public static final String KEY_INSTITUTE = "institute";
	public static final String KEY_SCHOOL = "school";
	public static final String KEY_CLASS = "cls";
	
	@Autowired
	private PlayGoEngineClientService engineService;
	
	@Autowired
	private InitiativeRepository initiativeRepo;
	
	@Autowired
	private PlayerTeamRepository teamRepo;
	
	@Autowired
	private AvatarService avatarService;
	
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
				final Set<TeamMember> members = teamRepo.findByInitiativeId(initiativeId)
						.stream()
						.map(t -> t.getMembers())
						.flatMap(Collection::stream)
						.collect(Collectors.toSet());
				final Set<String> registered = members
						.stream()
						.filter(t -> t.isSubscribed())
						.map(t -> t.getNickname())
						.collect(Collectors.toSet());
				page.getContent().forEach(p -> p.setSubscribed(registered.contains(p.getNickname())));
			}
			return page;
		}
		return Page.empty();
	}
	
	public PlayerTeam saveTeam(String initiativeId, PlayerTeam team) throws HSCError {
		String currentUser = securityHelper.getCurrentPreferredUsername();
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}
		
		List<PlayerTeam> list = teamRepo.findByNickname((String)team.getCustomData().get(KEY_NAME));
		if(list.size() > 0) {
			if(StringUtils.hasText(team.getId())) {
				if((list.size() > 1) || (!list.get(0).getId().equals(team.getId()))) {
					throw new DataException("NAME");
				}
			} else {
				throw new DataException("NAME");
			}			
		}

		List<Initiative> userInitiatives = getInitativesForManager();
		boolean isManager = userInitiatives.stream().anyMatch(i -> i.getInitiativeId().equals(initiativeId));
		team.setInitiativeId(initiativeId);
		
		Set<TeamMember> toRemove = new HashSet<>();
		Set<TeamMember> toAdd = new HashSet<>(team.getMembers());
		
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
		for(TeamMember tm : toAdd) {
			tm.setSubscribed(false);
		}
		team = teamRepo.save(team);
		// remove not used
		for (TeamMember tm : toRemove) {
			if(tm.isSubscribed()) {
				try {
					engineService.unsubscribe(initiative.getInitiativeId(), tm.getNickname());
				} catch (Exception e1) {
					logger.error("Failed to remove subscription", e1);
				}				
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
		
		Set<TeamMember> toRemove = new HashSet<>();
		
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
		for (TeamMember tm: toRemove) {
			if(tm.isSubscribed()) {
				try {
					engineService.unsubscribe(initiative.getInitiativeId(), tm.getNickname());
				} catch (Exception e1) {
					logger.error("Failed to remove subscription", e1);
				}				
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
		Set<TeamMember> registered = teamRepo.findByInitiativeId(initiative.getInitiativeId())
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
		List<PlayerTeam> result = new ArrayList<>();
		List<Initiative> initiatives = getInitativesForManager();
		if (initiatives.stream().anyMatch(i -> i.getInitiativeId().equals(initiativeId))) {
			result = teamRepo.findByInitiativeId(initiativeId); 
		} else {
			result = teamRepo.findByInitiativeIdAndOwner(initiativeId, securityHelper.getCurrentPreferredUsername());
		}
		result.forEach(t -> addSmallAvatar(t));
		return result;
	}
	
	public void forceSync() {
		if (isAdmin(engineService.getUserRoles())) {
			syncExternalCampaigns();
		}
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
		return roles != null && roles.stream().anyMatch(r -> r.getRole().equals(UserRole.Role.admin));
	}
	
	private List<String> getTerritories(List<UserRole> roles) {
		return roles.stream().filter(r -> r.getRole().equals(UserRole.Role.territory)).map(r -> r.getEntityId()).collect(Collectors.toList());
	}
	private List<String> getCampaigns(List<UserRole> roles) {
		return roles.stream().filter(r -> r.getRole().equals(UserRole.Role.campaign)).map(r -> r.getEntityId()).collect(Collectors.toList());
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
	
	public boolean checkSubscribeTeamMember(String initiativeId, String nickname) throws HSCError {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}
		final Set<TeamMember> members = teamRepo.findByInitiativeId(initiativeId)
				.stream()
				.map(t -> t.getMembers())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
		final Set<String> registered = members
				.stream()
				.map(t -> t.getNickname())
				.collect(Collectors.toSet());
		return registered.contains(nickname);
		
	}
	
	public String subscribeTeamMember(String initiativeId, String nickname) throws HSCError {
		if(securityHelper.checkAPIRole() || isAdmin(engineService.getUserRoles())) {
			Initiative initiative = getInitiative(initiativeId);
			if (initiative == null) {
				throw new NotFoundException("NO_INITIATIVE");
			}
			List<PlayerTeam> teams = teamRepo.findByInitiativeId(initiativeId);
			for(PlayerTeam team : teams) {
				for(TeamMember tm : team.getMembers()) {
					if(tm.getNickname().equals(nickname)) {
						if(tm.isSubscribed()) {
							throw new NotFoundException("PLAYER_ALREADY_SUBSCRIBED");
						}
						tm.setSubscribed(true);
						teamRepo.save(team);
						return team.getId(); 
					}
				}			
			}
			throw new NotFoundException("PLAYER_NOT_PRESENT");
		}
		throw new OperationNotEnabledException("SUBSCRIBE");
	}
	
	public List<PlayerInfo> getPlayerTeamInfo(String initiativeId, String teamId) throws HSCError {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}		
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if (team == null) {
			throw new NotFoundException("NO_TEAM");
		}
		List<String> players = team.getMembers().stream()
			.map(tm -> tm.getPlayerId())
			.collect(Collectors.toList());
		return engineService.getPlayersWithAvatars(initiative.getCampaign().getTerritoryId(), players);
	}

	public PlayerTeam getPublicTeamInfo(String initiativeId, String teamId) throws HSCError {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}		
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if (team == null) {
			throw new NotFoundException("NO_TEAM");
		}
		return getPublicTeamInfo(team);
	}
	
	public PlayerTeam getMyTeamInfo(String initiativeId, String teamId) throws HSCError {
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			throw new NotFoundException("NO_INITIATIVE");
		}		
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if (team == null) {
			throw new NotFoundException("NO_TEAM");
		}
		String playerId = securityHelper.getCurrentSubject();
		if(!team.getMembers().stream().anyMatch(m -> m.getPlayerId().equals(playerId))) {
			throw new OperationNotPermittedException("NO_TEAM");
		}
		Image avatar = avatarService.getTeamSmallAvatar(team.getId());
		if(avatar != null) {
			team.setAvatar(avatar);
		}
		List<String> players = team.getMembers().stream()
				.map(tm -> tm.getPlayerId())
				.collect(Collectors.toList());
		List<PlayerInfo> list = engineService.getPlayersWithAvatars(initiative.getCampaign().getTerritoryId(), players);
		Map<String, PlayerInfo> map = list.stream().collect(Collectors.toMap(PlayerInfo::getPlayerId, Function.identity()));
		team.getMembers().forEach(m -> {
			PlayerInfo playerInfo = map.get(m.getPlayerId());
			if((playerInfo != null) && (playerInfo.getAvatar() != null)) {
				m.setAvatar(playerInfo.getAvatar());
			}
		});		
		return team;
	}
	
	private PlayerTeam getPublicTeamInfo(PlayerTeam team) {
		PlayerTeam publicTeam = new PlayerTeam();
		publicTeam.setId(team.getId());
		publicTeam.setInitiativeId(team.getInitiativeId());
		publicTeam.setExpected(team.getExpected());
		if(team.getCustomData().containsKey(PlayerTeamService.KEY_NAME)) {
			publicTeam.getCustomData().put(PlayerTeamService.KEY_NAME, team.getCustomData().get(PlayerTeamService.KEY_NAME));
		}
		if(team.getCustomData().containsKey(PlayerTeamService.KEY_INSTITUTE)) {
			publicTeam.getCustomData().put(PlayerTeamService.KEY_INSTITUTE, team.getCustomData().get(PlayerTeamService.KEY_INSTITUTE));
		}
		if(team.getCustomData().containsKey(PlayerTeamService.KEY_SCHOOL)) {
			publicTeam.getCustomData().put(PlayerTeamService.KEY_SCHOOL, team.getCustomData().get(PlayerTeamService.KEY_SCHOOL));
		}
		if(team.getCustomData().containsKey(PlayerTeamService.KEY_CLASS)) {
			publicTeam.getCustomData().put(PlayerTeamService.KEY_CLASS, team.getCustomData().get(PlayerTeamService.KEY_CLASS));
		}
		Image avatar = avatarService.getTeamSmallAvatar(team.getId());
		if(avatar != null) {
			publicTeam.setAvatar(avatar);
		}
		return publicTeam;
	}
	
	private void addSmallAvatar(PlayerTeam team) {
		Image avatar = avatarService.getTeamSmallAvatar(team.getId());
		if(avatar != null) {
			team.setAvatar(avatar);
		}		
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

	public Object unsubscribeTeamMember(String initiativeId, String nickname) throws HSCError {
		// TODO Auto-generated method stub
		return null;
	}

}
