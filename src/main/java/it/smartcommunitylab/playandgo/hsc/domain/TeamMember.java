package it.smartcommunitylab.playandgo.hsc.domain;

public class TeamMember {
	private String playerId;
	private String nickname;
	private boolean subscribed = false;
	
	public TeamMember() {}
	
	public TeamMember(String playerId, String nickname) {
		this.playerId = playerId;
		this.nickname = nickname;
	}
	
	public String getPlayerId() {
		return playerId;
	}
	public void setPlayerId(String playerId) {
		this.playerId = playerId;
	}
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public boolean isSubscribed() {
		return subscribed;
	}
	public void setSubscribed(boolean subscribed) {
		this.subscribed = subscribed;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof TeamMember) {
			return getNickname().equals(((TeamMember)obj).getNickname()); 
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getNickname().hashCode();
	}
}
