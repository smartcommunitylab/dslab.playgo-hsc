package it.smartcommunitylab.playandgo.hsc.domain;

import org.bson.types.Binary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="avatars")
public class Avatar {
	
	@Id
	private String id;
	
	private Binary avatarData;
	private Binary avatarDataSmall;
	
	private String contentType;
	private String fileName;
	
	private String avatarSmallUrl;
	private String avatarUrl;
	
	@Indexed
	private String teamId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Binary getAvatarData() {
		return avatarData;
	}

	public void setAvatarData(Binary avatarData) {
		this.avatarData = avatarData;
	}

	public Binary getAvatarDataSmall() {
		return avatarDataSmall;
	}

	public void setAvatarDataSmall(Binary avatarDataSmall) {
		this.avatarDataSmall = avatarDataSmall;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getAvatarSmallUrl() {
		return avatarSmallUrl;
	}

	public void setAvatarSmallUrl(String avatarSmallUrl) {
		this.avatarSmallUrl = avatarSmallUrl;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

}
