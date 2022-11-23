package it.smartcommunitylab.playandgo.hsc.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Campaign {
	private String campaignId;
	private String territoryId;
	private Map<String, String> name = new HashMap<>();
	private Map<String, String> description = new HashMap<>();
	private Long dateFrom;
	private Long dateTo;
	private Boolean active = Boolean.FALSE;
	private Boolean communications = Boolean.FALSE;
	private Boolean visible = Boolean.FALSE;
	private int startDayOfWeek = 1; //Monday is 1 and Sunday is 7
	private String gameId;
	private Map<String, List<CampaignDetail>> details = new HashMap<>(); 
	private Image logo;
	private Image banner;
	
	private Map<String, Object> validationData = new HashMap<>();
	private Map<String, Object> specificData = new HashMap<>();

	public boolean currentlyActive() {
		return !Boolean.FALSE.equals(getActive()) && 
				(getDateFrom() == null || getDateFrom() <= System.currentTimeMillis()) &&
				(getDateTo() == null || getDateTo() >=  System.currentTimeMillis());
	}

	public String getCampaignId() {
		return campaignId;
	}

	public void setCampaignId(String campaignId) {
		this.campaignId = campaignId;
	}

	public String getTerritoryId() {
		return territoryId;
	}

	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}

	public Map<String, String> getName() {
		return name;
	}

	public void setName(Map<String, String> name) {
		this.name = name;
	}

	public Map<String, String> getDescription() {
		return description;
	}

	public void setDescription(Map<String, String> description) {
		this.description = description;
	}

	public Long getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Long dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Long getDateTo() {
		return dateTo;
	}

	public void setDateTo(Long dateTo) {
		this.dateTo = dateTo;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}

	public Boolean getCommunications() {
		return communications;
	}

	public void setCommunications(Boolean communications) {
		this.communications = communications;
	}

	public Boolean getVisible() {
		return visible;
	}

	public void setVisible(Boolean visible) {
		this.visible = visible;
	}

	public int getStartDayOfWeek() {
		return startDayOfWeek;
	}

	public void setStartDayOfWeek(int startDayOfWeek) {
		this.startDayOfWeek = startDayOfWeek;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public Map<String, List<CampaignDetail>> getDetails() {
		return details;
	}

	public void setDetails(Map<String, List<CampaignDetail>> details) {
		this.details = details;
	}

	public Image getLogo() {
		return logo;
	}

	public void setLogo(Image logo) {
		this.logo = logo;
	}

	public Image getBanner() {
		return banner;
	}

	public void setBanner(Image banner) {
		this.banner = banner;
	}

	public Map<String, Object> getValidationData() {
		return validationData;
	}

	public void setValidationData(Map<String, Object> validationData) {
		this.validationData = validationData;
	}

	public Map<String, Object> getSpecificData() {
		return specificData;
	}

	public void setSpecificData(Map<String, Object> specificData) {
		this.specificData = specificData;
	}

}
