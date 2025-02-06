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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

/**
 * @author raman
 *
 */
public class PlayerTeam {

	@Id
	private String id;
	private String initiativeId;
	private String owner;
	private String type;
	private Set<TeamMember> members = Collections.emptySet();
	private Integer expected;
	
	private Map<String, Object> customData = new HashMap<>();
	
	@Transient
	private Image avatar;
	
	@Transient
	private int numMembers;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public Set<TeamMember> getMembers() {
		return members;
	}
	public void setMembers(Set<TeamMember> members) {
		this.members = members;
	}
	public Map<String, Object> getCustomData() {
		return customData;
	}
	public void setCustomData(Map<String, Object> customData) {
		this.customData = customData;
	}
	public Integer getExpected() {
		return expected;
	}
	public void setExpected(Integer expected) {
		this.expected = expected;
	}
	public String getInitiativeId() {
		return initiativeId;
	}
	public void setInitiativeId(String initiativeId) {
		this.initiativeId = initiativeId;
	}
	public Image getAvatar() {
		return avatar;
	}
	public void setAvatar(Image avatar) {
		this.avatar = avatar;
	}
	public int getNumMembers() {
		return numMembers;
	}
	public void setNumMembers(int numMembers) {
		this.numMembers = numMembers;
	}
	
	@Override
	public String toString() {
		return id + "_" + initiativeId;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
}
