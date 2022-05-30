package it.smartcommunitylab.playandgo.hsc.domain;

import java.util.HashMap;
import java.util.Map;

public class CampaignSubscription {
	private String id;
	private String playerId;
	private String campaignId;
	private String territoryId;
	private String mail;
	private Boolean sendMail = Boolean.FALSE;
	private String registrationDate;
	private Map<String, Object> campaignData = new HashMap<>();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getCampaignId() {
		return campaignId;
	}
	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public Map<String, Object> getCampaignData() {
		return campaignData;
	}
	public void setCampaignData(Map<String, Object> campaignData) {
		this.campaignData = campaignData;
	}
	public Boolean getSendMail() {
		return sendMail;
	}
	public void setSendMail(Boolean sendMail) {
		this.sendMail = sendMail;
	}
	public String getTerritoryId() {
		return territoryId;
	}
	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
	public String getRegistrationDate() {
		return registrationDate;
	}
	public void setRegistrationDate(String registrationDate) {
		this.registrationDate = registrationDate;
	}
}