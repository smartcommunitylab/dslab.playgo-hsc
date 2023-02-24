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

package it.smartcommunitylab.playandgo.hsc.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

/**
 * @author raman
 *
 */
public class Initiative {

	@Id
	private String initiativeId;
	
	private String type;
	
	private Campaign campaign;
	private Boolean canCreate;
	private Boolean canEdit;
	
	private Double bonus;
	private Double bonusThreshold;
	private Integer maxTeamSize;
	
	private List<String> teamLeaderDomainList = new ArrayList<>();
	private List<String> teamLeaderList = new ArrayList<>();
	
	public String getInitiativeId() {
		return initiativeId;
	}
	public void setInitiativeId(String initiativeId) {
		this.initiativeId = initiativeId;
	}
	public Campaign getCampaign() {
		return campaign;
	}
	public void setCampaign(Campaign campaign) {
		this.campaign = campaign;
	}
	public Boolean getCanCreate() {
		return canCreate;
	}
	public void setCanCreate(Boolean canCreate) {
		this.canCreate = canCreate;
	}
	public Boolean getCanEdit() {
		return canEdit;
	}
	public void setCanEdit(Boolean canEdit) {
		this.canEdit = canEdit;
	}
	public Double getBonus() {
		return bonus;
	}
	public void setBonus(Double bonus) {
		this.bonus = bonus;
	}
	public Double getBonusThreshold() {
		return bonusThreshold;
	}
	public void setBonusThreshold(Double bonusThreshold) {
		this.bonusThreshold = bonusThreshold;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List<String> getTeamLeaderDomainList() {
		return teamLeaderDomainList;
	}
	public void setTeamLeaderDomainList(List<String> teamLeaderDomainList) {
		this.teamLeaderDomainList = teamLeaderDomainList;
	}
	public List<String> getTeamLeaderList() {
		return teamLeaderList;
	}
	public void setTeamLeaderList(List<String> teamLeaderList) {
		this.teamLeaderList = teamLeaderList;
	}
	public Integer getMaxTeamSize() {
		return maxTeamSize;
	}
	public void setMaxTeamSize(Integer maxTeamSize) {
		this.maxTeamSize = maxTeamSize;
	}
	
	@Override
	public String toString() {
		return initiativeId + "_" + type;
	}
}
