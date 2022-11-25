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

import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;

/**
 * @author raman
 *
 */
public class PlayerTeam {

	@Id
	private String id;
	private String initiativeId;
	private String owner;
	private String name;
	private String institute;
	private String school;
	private String className;
	private Set<TeamMember> members;
	private Integer expected;
	
	private Map<String, Object> customData;
	
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
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getInstitute() {
		return institute;
	}
	public void setInstitute(String institute) {
		this.institute = institute;
	}
	public String getSchool() {
		return school;
	}
	public void setSchool(String school) {
		this.school = school;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
}
