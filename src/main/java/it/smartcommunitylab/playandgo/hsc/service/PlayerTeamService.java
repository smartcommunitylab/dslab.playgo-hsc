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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.playandgo.hsc.domain.Avatar;
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
import it.smartcommunitylab.playandgo.hsc.ge.GamificationEngineService;
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
	public static final String KEY_DESC = "desc";
	public static final String KEY_TEAM_MAX_NUM = "maxMembers";
	public static final String KEY_TEAM_NUM = "currentPlayers";
	
	@Autowired
	private PlayGoEngineClientService engineService;
	
	@Autowired
	private InitiativeRepository initiativeRepo;
	
	@Autowired
	private PlayerTeamRepository teamRepo;
	
	@Autowired
	private AvatarService avatarService;
	
	@Autowired
	private GamificationEngineService gamificationEngineService;
	
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
		String email = securityHelper.getCurrentPreferredUsername();
		List<UserRole> roles = engineService.getUserRoles();
		logger.info(String.format("getInitativesForManager roles[%s]:%s", email, roles));
		if (isAdmin(roles)) return initiativeRepo.findAll();
		List<String> territories = getTerritories(roles);
		logger.info(String.format("getInitativesForManager territories[%s]:%s", email, territories));
		if ((territories != null) && (territories.size() > 0)) return initiativeRepo.findByTerritories(territories);
		List<String> campaigns = getCampaigns(roles);
		logger.info(String.format("getInitativesForManager campaigns[%s]:%s", email, campaigns));
		if ((campaigns != null) && (campaigns.size() > 0)) return initiativeRepo.findByCampaignIds(campaigns);
		return Collections.emptyList();
	}
	
	public boolean isCampaignManager(String initiativeId) {
		Initiative initiative = initiativeRepo.findById(initiativeId).orElse(null);
		if(initiative == null) {
			return false;
		}
		List<UserRole> roles = engineService.getUserRoles();
		if (isAdmin(roles)) 
			return true;
		return roles.stream().anyMatch(r -> {
			if(r.getRole().equals(UserRole.Role.territory) && r.getEntityId().equals(initiative.getCampaign().getTerritoryId()))
				return true;
			if(r.getRole().equals(UserRole.Role.campaign) && r.getEntityId().equals(initiative.getCampaign().getCampaignId()))
				return true;
			return false;
		});
	}
	
	public boolean isTeamManager(String initiativeId) {
		String email = securityHelper.getCurrentPreferredUsername();
		Initiative initiative = initiativeRepo.findById(initiativeId).orElse(null);
		return isTeamManager(initiative, email);
	}
	
	public boolean isTeamManager(Initiative initiative, String email) {
		if(initiative == null) {
			return false;
		}
		boolean match = initiative.getTeamLeaderDomainList().stream().anyMatch(r -> {
			return email.toLowerCase().endsWith(r.toLowerCase());
		});
		if(match)
			return true;
		match = initiative.getTeamLeaderList().stream().anyMatch(r -> {
			return email.toLowerCase().equals(r.toLowerCase());
		});
		if(match)
			return true;
		return false;		
	}
	
	public List<Initiative> getTeamLeaderInitiatives() {
		String email = securityHelper.getCurrentPreferredUsername();
		List<Initiative> list = initiativeRepo.findAll();
		List<Initiative> result = list.stream().filter(i -> i.getCampaign().getActive()  && isTeamManager(i, email)).collect(Collectors.toList());
		logger.info(String.format("getTeamLeaderInitiatives [%s]:%s", email, result));		
		return result;
	}
	
	public List<PlayerTeam> getPlayerTeamByOwner() {
		String email = securityHelper.getCurrentPreferredUsername();
		List<PlayerTeam> list = teamRepo.findByOwner(email);
		List<PlayerTeam> result = new ArrayList<>();
		list.forEach(t -> {
			Initiative initiative = initiativeRepo.findById(t.getInitiativeId()).orElse(null);
			if((initiative != null) && initiative.getCampaign().getActive() && isTeamManager(initiative, email)) {
				result.add(t);
			}
		});
		result.forEach(t -> addSmallAvatar(t));
		logger.info(String.format("getPlayerTeamByOwner [%s]:%s", email, result));
		return result;
	}
	
	public boolean isMyTeam(String teamId) {
		String email = securityHelper.getCurrentPreferredUsername();
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if(team != null) {
			return team.getOwner().equals(email);
		}
		return false;
	}
	
	public boolean isRunning(String initiativeId) {
		Initiative initiative = initiativeRepo.findById(initiativeId).orElse(null);
		if(initiative != null) {
			long millis = System.currentTimeMillis();
			return (initiative.getCampaign().getDateFrom() <= millis) && (initiative.getCampaign().getDateTo() >= millis);
		}
		return false;
	}

	public Page<PlayerInfo> searchPlayers(String initiativeId, String txt, Pageable pageRequest) throws HSCError {
		logger.info(String.format("searchPlayers[%s]:%s", initiativeId, txt));
		try {
			if(isCampaignManager(initiativeId) || isTeamManager(initiativeId)) {
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
								//.filter(t -> t.isSubscribed())
								.map(t -> t.getNickname())
								.collect(Collectors.toSet());
						List<PlayerInfo> result = new ArrayList<>();
						page.getContent().forEach(p -> {
							if(!registered.contains(p.getNickname())) {
								result.add(p);
							}
						});
						return new PageImpl<>(result, pageRequest, result.size());
					}
					return page;
				}			
			}
			return Page.empty();			
		} catch (Exception e) {
			logger.error(String.format("searchPlayers[%s]:ENGINE_SEARCH %s", initiativeId, txt));
			throw new DataException("ENGINE_SEARCH:" + e.getMessage());
		}
	}
	
	private String escapeRegEx(String s) {
		return s.replace("*", "\\*").replace("+", "\\+").replace("?", "\\?").replace(".", "\\.").replace("^", "\\^").replace("$", "\\$");
	}
	
	public PlayerTeam saveTeam(String initiativeId, PlayerTeam team) throws HSCError {
		logger.info(String.format("saveTeam[%s]:%s - %s", initiativeId, team.getId(), team.getCustomData().get(KEY_NAME)));
		String currentUser = securityHelper.getCurrentPreferredUsername();
		boolean isCampaignManager = isCampaignManager(initiativeId);
		boolean isTeamManager = isTeamManager(initiativeId);
		if(!isCampaignManager && !isTeamManager) {
			logger.error(String.format("saveTeam[%s]:TEAM NO ROLE", initiativeId));
			throw new OperationNotPermittedException("TEAM");
		}
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			logger.error(String.format("saveTeam[%s]:NO_INITIATIVE", initiativeId));
			throw new NotFoundException("NO_INITIATIVE");
		}
		String name = (String)team.getCustomData().get(KEY_NAME);
		name = escapeRegEx(name);
		List<PlayerTeam> list = teamRepo.findByNickname(name);
		if(list.size() > 0) {
			if(StringUtils.hasText(team.getId())) {
				if((list.size() > 1) || (!list.get(0).getId().equals(team.getId()))) {
					logger.error(String.format("saveTeam[%s]:NAME %s", initiativeId, name));
					throw new DataException("NAME");
				}
			} else {
				logger.error(String.format("saveTeam[%s]:NAME %s", initiativeId, name));
				throw new DataException("NAME");
			}			
		}
				
		team.setInitiativeId(initiativeId);
		boolean isRunning = isRunning(initiativeId);
		
		Set<TeamMember> toRemove = new HashSet<>();
		Set<TeamMember> toAdd = new HashSet<>(team.getMembers());
		
		if (team.getId() != null) {
			PlayerTeam existing = teamRepo.findById(team.getId()).orElse(null);
			if (existing == null) {
				logger.error(String.format("saveTeam[%s]:NO_TEAM %s", initiativeId, team.getId()));
				throw new NotFoundException("NO_TEAM");
			}

			if (!isCampaignManager && !existing.getOwner().equals(currentUser)) {
				// throw access exception
				logger.error(String.format("saveTeam[%s]:OWNER %s", initiativeId, team.getId()));
				throw new OperationNotPermittedException("OWNER");
			} 
			
			if (!Boolean.TRUE.equals(initiative.getCanEdit())) {
				logger.error(String.format("saveTeam[%s]:EDIT %s", initiativeId, team.getId()));
				throw new OperationNotEnabledException("EDIT");
			}
			//check existing member subscribed flag
			for(TeamMember tm : team.getMembers()) {
				for(TeamMember etm : existing.getMembers()) {
					if(tm.getNickname().equals(etm.getNickname())) {
						tm.setSubscribed(etm.isSubscribed());
						tm.setUnregistered(etm.isUnregistered());
						break;
					}
				}
			}
				
			toRemove = new HashSet<>(existing.getMembers());
			toRemove.removeAll(team.getMembers());
			toAdd.removeAll(existing.getMembers());
			if(isRunning && !isAdmin()) {
				team.getCustomData().put(KEY_NAME, existing.getCustomData().get(KEY_NAME));
				team.setExpected(existing.getExpected());
			}
		} else {
			if (!Boolean.TRUE.equals(initiative.getCanCreate())) {
				throw new OperationNotEnabledException("CREATE");
			}
			team.setId(UUID.randomUUID().toString());
			team.setOwner(currentUser);
			if(!gamificationEngineService.createPlayer(team.getId(), initiative.getCampaign().getGameId(), true)) {
				logger.error(String.format("saveTeam[%s]:GAMIFICATION_TEAM ADD %s", initiativeId, team.getId()));
				throw new DataException("GAMIFICATION_TEAM");
			}
			try {
				engineService.addGroup(team.getId(), initiative.getInitiativeId());
			} catch (Exception e) {
				logger.error(String.format("saveTeam[%s]:ENGINE_TEAM ADD %s", initiativeId, team.getId()));
				throw new DataException("ENGINE_TEAM");
			}
		}
		validate(team, initiative);
		for(TeamMember tm : toAdd) {
			tm.setSubscribed(false);
		}
		// remove not used
		for (TeamMember tm : toRemove) {
			if(tm.isSubscribed()) {
				if(!gamificationEngineService.removePlayerToGroup(tm.getPlayerId(), team.getId(), initiative.getCampaign().getGameId())) {
					logger.error(String.format("saveTeam[%s]:GAMIFICATION_PLAYER REMOVE %s", initiativeId, tm.getPlayerId()));
					throw new DataException("GAMIFICATION_PLAYER");
				}						
				try {
					engineService.unsubscribe(initiative.getInitiativeId(), tm.getNickname());
				} catch (Exception e1) {
					logger.error(String.format("saveTeam[%s]:ENGINE_PLAYER REMOVE %s", initiativeId, tm.getPlayerId()));
					logger.error("Failed to remove subscription", e1);
				}				
			}
		}
		setGameMetadata(team, initiative);							
		team = teamRepo.save(team);
		return team;
	}

	private void setGameMetadata(PlayerTeam team, Initiative initiative) throws DataException {
		Map<String, Object> customData = new HashMap<>();
		customData.put(KEY_TEAM_MAX_NUM, team.getExpected());
		customData.put(KEY_TEAM_NUM, team.getMembers().size());
		if(!gamificationEngineService.changeCustomData(team.getId(), initiative.getCampaign().getGameId(), customData)) {
			logger.error(String.format("setGameMetadata[%s]:GAMIFICATION_CUSTOM_DATA %s", initiative.getInitiativeId(), team.getId()));
			throw new DataException("GAMIFICATION_CUSTOM_DATA");
		}
	}
	
	public void deleteTeam(String initiativeId, String teamId) throws HSCError {
		logger.info(String.format("deleteTeam[%s]:%s", initiativeId, teamId));
		if(!isAdmin()) {
			logger.error(String.format("deleteTeam[%s]:TEAM NO ROLE", initiativeId));
			throw new OperationNotPermittedException("TEAM");
		}
		
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			logger.error(String.format("deleteTeam[%s]:NO_INITIATIVE", initiativeId));
			throw new NotFoundException("NO_INITIATIVE");
		}
		
		PlayerTeam existing = teamRepo.findById(teamId).orElse(null);
		if (existing == null) {
			logger.error(String.format("deleteTeam[%s]:NO_TEAM %s", initiativeId, teamId));
			throw new NotFoundException("NO_TEAM");
		}
		
		if(existing.getMembers().size() > 0) {
			logger.error(String.format("deleteTeam[%s]:TEAM_NOT_EMPTY %s", initiativeId, teamId));
			throw new OperationNotEnabledException("TEAM_NOT_EMPTY");
		}

		if(!gamificationEngineService.deleteGroup(existing.getId(), initiative.getCampaign().getGameId())) {
			logger.error(String.format("deleteTeam[%s]:GAMIFICATION_TEAM REMOVE %s", initiativeId, teamId));
			throw new DataException("GAMIFICATION_TEAM");
		}
		try {
			engineService.deleteGroup(teamId);
		} catch (Exception e) {
			logger.error(String.format("deleteTeam[%s]:ENGINE_PLAYER REMOVE %s", initiativeId, teamId));
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
			logger.error(String.format("validate[%s]:PLAYER %s", initiative.getInitiativeId(), team.getId()));
			throw new DataException("PLAYER");
		}
	}

	public Initiative getInitiativeWeb(String initiativeId) {
		if(isCampaignManager(initiativeId))
			return initiativeRepo.findById(initiativeId).orElse(null);
		return null;
	}
	
	public Initiative getInitiative(String initiativeId) {
		return initiativeRepo.findById(initiativeId).orElse(null);
	}
	
	public void setInitiativeCreate(String initiativeId, boolean value) {
		logger.info(String.format("setInitiativeCreate[%s]", initiativeId));
		List<Initiative> initiatives = getInitativesForManager();
		initiatives.stream().filter(i -> i.getInitiativeId().equals(initiativeId)).forEach(i -> {
			i.setCanCreate(value);
			initiativeRepo.save(i);
		});
	}
	
	public void setInitiativeEdit(String initiativeId, boolean value) {
		logger.info(String.format("setInitiativeEdit[%s]", initiativeId));
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
	public Initiative saveInitiative(String initiativeId, Initiative initiative) throws HSCError {
		logger.info(String.format("saveInitiative[%s]", initiativeId));
		if(!isCampaignManager(initiativeId)) {
			throw new OperationNotPermittedException("CAMPAIGN");
		}
		Initiative old = initiativeRepo.findById(initiativeId).orElse(null);
		if (old != null) {
			//old.setBonus(initiative.getBonus());
			//old.setBonusThreshold(initiative.getBonusThreshold());
			old.setMaxTeamSize(initiative.getMaxTeamSize());
			old.setMinTeamSize(initiative.getMinTeamSize());
			old.setTeamLeaderDomainList(initiative.getTeamLeaderDomainList());
			old.setTeamLeaderList(initiative.getTeamLeaderList());
			old.setTeamTypeList(initiative.getTeamTypeList());
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
	
	@Scheduled(fixedDelay=1000*60*60*4, initialDelay = 1000*60*5) 
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
				initiative.setMaxTeamSize(30);
				initiative.setType("hsc");
			} else {
				String gameId = initiative.getCampaign().getGameId();
				if((gameId != null) && (!gameId.equals(c.getGameId()))) {
					List<PlayerTeam> list = teamRepo.findByInitiativeId(initiative.getInitiativeId());
					if(list.size() > 0) {
						logger.error(String.format("syncExternalCampaigns align gameId error: %s - %s - %s", initiative.getInitiativeId(), gameId, c.getGameId()));
						c.setGameId(gameId);
					}
				}
			}
			initiative.setCampaign(c);
			initiativeRepo.save(initiative);
		}
	}
	
	public boolean isAdmin() {
		List<UserRole> roles = engineService.getUserRoles();
		return isAdmin(roles);
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
		logger.info(String.format("checkSubscribeTeamMember[%s]:%s", initiativeId, nickname));
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
	
	public synchronized String subscribeTeamMember(String initiativeId, String nickname, String teamId) throws HSCError {
		logger.info(String.format("subscribeTeamMember[%s]:%s", initiativeId, nickname));
		if(securityHelper.checkAPIRole() || isAdmin(engineService.getUserRoles())) {
			Initiative initiative = getInitiative(initiativeId);
			if (initiative == null) {
				logger.error(String.format("subscribeTeamMember[%s]:NO_INITIATIVE", initiativeId));
				throw new NotFoundException("NO_INITIATIVE");
			}
			List<PlayerTeam> teams = teamRepo.findByInitiativeId(initiativeId);
			for(PlayerTeam team : teams) {
				for(TeamMember tm : team.getMembers()) {
					if(tm.getNickname().equals(nickname)) {
						if(tm.isSubscribed()) {
							logger.error(String.format("subscribeTeamMember[%s]:PLAYER_ALREADY_SUBSCRIBED %s", initiativeId, nickname));
							throw new DataException("PLAYER_ALREADY_SUBSCRIBED");
						}
						if(!team.getId().equals(teamId)) {
							logger.error(String.format("subscribeTeamMember[%s]:TEAM_NOT_CORRECT %s", initiativeId, teamId));
							throw new DataException("TEAM_NOT_CORRECT");
						}
						if(!gamificationEngineService.createPlayer(tm.getPlayerId(), initiative.getCampaign().getGameId(), false)) {
							logger.error(String.format("subscribeTeamMember[%s]:GAMIFICATION-PLAYER %s", initiativeId, nickname));
							throw new DataException("GAMIFICATION-PLAYER");
						}
						if(!gamificationEngineService.addPlayerToGroup(tm.getPlayerId(), team.getId(), initiative.getCampaign().getGameId())) {
							logger.error(String.format("subscribeTeamMember[%s]:GAMIFICATION-PLAYER-TEAM %s", initiativeId, nickname));
							throw new DataException("GAMIFICATION-PLAYER-TEAM");
						}												
						tm.setSubscribed(true);
						teamRepo.save(team);
						return team.getId(); 
					}
				}			
			}
			logger.error(String.format("subscribeTeamMember[%s]:PLAYER_NOT_PRESENT %s", initiativeId, nickname));
			throw new NotFoundException("PLAYER_NOT_PRESENT");
		}
		logger.error(String.format("subscribeTeamMember[%s]:SUBSCRIBE NO ROLE", initiativeId));
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
	
	public List<PlayerTeam> getPublicTeamsInfo(String initiativeId) throws HSCError {
		List<PlayerTeam> result = new ArrayList<>();
		List<PlayerTeam> list = teamRepo.findByInitiativeId(initiativeId);
		for(PlayerTeam team : list) {
			result.add(getPublicTeamInfo(team));
		}
		return result;
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
	
	public Avatar uploadTeamAvatar(String teamId, MultipartFile data) throws HSCError {
		logger.info(String.format("uploadTeamAvatar[%s]", teamId));
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if(team == null) {
			logger.info(String.format("uploadTeamAvatar[%s]:NO_TEAM", teamId));
			throw new NotFoundException("NO_TEAM");
		}
		if(!isCampaignManager(team.getInitiativeId()) && !isMyTeam(teamId)) {
			logger.info(String.format("uploadTeamAvatar[%s]:TEAM NO ROLE", teamId));
			throw new OperationNotPermittedException("TEAM");
		}
		return avatarService.uploadTeamAvatar(teamId, data);
	}
	
	private PlayerTeam getPublicTeamInfo(PlayerTeam team) {
		PlayerTeam publicTeam = new PlayerTeam();
		publicTeam.setId(team.getId());
		publicTeam.setInitiativeId(team.getInitiativeId());
		publicTeam.setExpected(team.getExpected());
		publicTeam.setNumMembers(team.getMembers().size());
		if(team.getCustomData().containsKey(PlayerTeamService.KEY_NAME)) {
			publicTeam.getCustomData().put(PlayerTeamService.KEY_NAME, team.getCustomData().get(PlayerTeamService.KEY_NAME));
		}
		if(team.getCustomData().containsKey(PlayerTeamService.KEY_DESC)) {
			publicTeam.getCustomData().put(PlayerTeamService.KEY_DESC, team.getCustomData().get(PlayerTeamService.KEY_DESC));
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
		logger.info(String.format("unsubscribeTeamMember[%s]:%s", initiativeId, nickname));
		return null;
	}
	
	public synchronized void unregisterPlayer(String initiativeId, String playerId, String nickname) throws HSCError {
		logger.info(String.format("unregisterPlayer[%s]:%s", initiativeId, nickname));
		if(securityHelper.checkAPIRole() || isAdmin(engineService.getUserRoles())) {
			Initiative initiative = getInitiative(initiativeId);
			if (initiative == null) {
				logger.error(String.format("unregisterPlayer[%s]:NO_INITIATIVE", initiativeId));
				throw new NotFoundException("NO_INITIATIVE");
			}
			List<PlayerTeam> list = teamRepo.findByInitiativeId(initiativeId);
			for(PlayerTeam team : list) {
				boolean save = false;
				for(TeamMember tm : team.getMembers()) {
					if(tm.getPlayerId().equals(playerId)) {
						tm.setNickname(nickname);
						tm.setUnregistered(true);
						if(tm.isSubscribed()) {
							tm.setSubscribed(false);
							if(!gamificationEngineService.removePlayerToGroup(playerId, team.getId(), initiative.getCampaign().getGameId())) {
								logger.error(String.format("unregisterPlayer[%s]:GAMIFICATION_PLAYER REMOVE %s - %s", initiativeId, playerId, team.getId()));
								throw new DataException("GAMIFICATION_TEAM");
							}
						}
						save = true;
					}					
				}
				if(save) {
					teamRepo.save(team);
				}
			}
		}
		logger.error(String.format("unregisterPlayer[%s]:UNREGISTER NO ROLE", initiativeId));
		throw new OperationNotEnabledException("UNREGISTER");		
	}
	
	public void removePlayerFromTeam(String initiativeId, String teamId, String playerId) throws HSCError {
		logger.info(String.format("removePlayerFromTeam[%s]:%s - %s", initiativeId, teamId, playerId));
		if(!isAdmin()) {
			logger.error(String.format("removePlayerFromTeam[%s]:REMOVE_PLAYER NO ROLE", initiativeId));
			throw new OperationNotPermittedException("REMOVE_PLAYER");
		}
		Initiative initiative = getInitiative(initiativeId);
		if (initiative == null) {
			logger.error(String.format("removePlayerFromTeam[%s]:NO_INITIATIVE", initiativeId));
			throw new NotFoundException("NO_INITIATIVE");
		}
		PlayerTeam team = teamRepo.findById(teamId).orElse(null);
		if(team == null) {
			logger.error(String.format("removePlayerFromTeam[%s]:NO_TEAM %s", initiativeId, teamId));
			throw new NotFoundException("NO_TEAM");
		}
		TeamMember toRemove = null;
		for(TeamMember tm : team.getMembers()) {
			if(tm.getPlayerId().equals(playerId)) {
				if(tm.isSubscribed() || tm.isUnregistered()) {
					//remove play&go data
					try {
						engineService.removePlayerFromGroup(playerId, initiative.getInitiativeId());
					} catch (Exception e) {
						logger.error(String.format("removePlayerFromTeam[%s]:ENGINE_TEAM REMOVE %s", initiativeId, playerId));
						throw new DataException("ENGINE_TEAM");
					}
					//remove from team in GE
					if(!gamificationEngineService.removePlayerToGroup(playerId, team.getId(), initiative.getCampaign().getGameId())) {
						logger.error(String.format("removePlayerFromTeam[%s]:GAMIFICATION_TEAM GROUP REMOVE %s - %s", initiativeId, playerId, teamId));
						throw new DataException("GAMIFICATION_TEAM");
					}
					//remove player status in GE
					if(!gamificationEngineService.removePlayer(playerId, initiative.getCampaign().getGameId())) {
						logger.error(String.format("removePlayerFromTeam[%s]:GAMIFICATION_TEAM PLAYER REMOVE %s", initiativeId, playerId));
						throw new DataException("GAMIFICATION_TEAM");
					}
				}
				toRemove = tm;
				break;
			}
		}
		if(toRemove != null) {
			team.getMembers().remove(toRemove);
			setGameMetadata(team, initiative);
			teamRepo.save(team);
		}
	}

}
