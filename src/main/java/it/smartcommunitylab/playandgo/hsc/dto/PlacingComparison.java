package it.smartcommunitylab.playandgo.hsc.dto;

public class PlacingComparison {
	private CampaignPlacing prevPlacing;
	private CampaignPlacing myPlacing;
	private CampaignPlacing nextPlacing;
	
	public CampaignPlacing getPrevPlacing() {
		return prevPlacing;
	}
	public void setPrevPlacing(CampaignPlacing prevPlacing) {
		this.prevPlacing = prevPlacing;
	}
	public CampaignPlacing getMyPlacing() {
		return myPlacing;
	}
	public void setMyPlacing(CampaignPlacing myPlacing) {
		this.myPlacing = myPlacing;
	}
	public CampaignPlacing getNextPlacing() {
		return nextPlacing;
	}
	public void setNextPlacing(CampaignPlacing nextPlacing) {
		this.nextPlacing = nextPlacing;
	}
	
}
