package it.smartcommunitylab.playandgo.hsc.ge;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.smartcommunitylab.playandgo.hsc.util.HTTPConnector;
import it.smartcommunitylab.playandgo.hsc.util.JsonUtils;

@Service
public class GamificationEngineService {
	private static final Logger logger = LoggerFactory.getLogger(GamificationEngineService.class);
	
	@Value("${gamification.url}")
	private String gamificationUrl;
	
	@Value("${gamification.user}")
	private String gamificationUser;
	
	@Value("${gamification.password}")
	private String gamificationPassword;

	@Value("${gamification.secretKey1}")
	private String secretKey1;
	
	@Value("${gamification.secretKey2}")
	private String secretKey2;
	
	ObjectMapper mapper = new ObjectMapper();
	
	EncryptDecrypt cryptUtils;
	
	@PostConstruct
	public void init() throws Exception {
		cryptUtils = new EncryptDecrypt(secretKey1, secretKey2);
	}
	
	public boolean createPlayer(String playerId, String gameId, boolean isGroup) {
		try {
			Map<String, Object> data = new HashMap<>();
			data.put("gameId", gameId);
			data.put("playerId", playerId);
			data.put("name", playerId);
			String content = JsonUtils.toJSON(data);			
			String url = gamificationUrl + "/data/game/" + gameId;
			if(isGroup) {
				url = url + "/team/" + URLEncoder.encode(playerId, "UTF-8");
			} else {
				url = url + "/player/" + URLEncoder.encode(playerId, "UTF-8");
			}
			ResponseEntity<String> entity = HTTPConnector.doBasicAuthenticationMethod(url, content, "application/json", 
					"application/json", gamificationUser, gamificationPassword, HttpMethod.POST);
			if (entity.getStatusCode().is2xxSuccessful() || entity.getStatusCode().is4xxClientError()) {
				return true;
			}
		} catch (Exception e) {
			logger.error(String.format("createPlayer error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return false;		
	}
	
	public boolean addPlayerToGroup(String playerId, String groupId, String gameId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/team/" + URLEncoder.encode(groupId, "UTF-8") 
				+ "/members/" + URLEncoder.encode(playerId, "UTF-8");			
			ResponseEntity<String> entity = HTTPConnector.doBasicAuthenticationMethod(url, null, "application/json", 
					"application/json", gamificationUser, gamificationPassword, HttpMethod.PUT);
			if (entity.getStatusCode().is2xxSuccessful()) {
				return true;
			}
		} catch (Exception e) {
			logger.error(String.format("createPlayer error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return false;		
	}
	
	public boolean removePlayerToGroup(String playerId, String groupId, String gameId) {
		try {
			String url = gamificationUrl + "/data/game/" + gameId + "/team/" + URLEncoder.encode(groupId, "UTF-8") 
				+ "/members/" + URLEncoder.encode(playerId, "UTF-8");			
			ResponseEntity<String> entity = HTTPConnector.doBasicAuthenticationMethod(url, null, "application/json", 
					"application/json", gamificationUser, gamificationPassword, HttpMethod.DELETE);
			if (entity.getStatusCode().is2xxSuccessful()) {
				return true;
			}
		} catch (Exception e) {
			logger.error(String.format("createPlayer error: %s - %s - %s", gameId, playerId, e.getMessage()));
		}
		return false;		
	}

}
