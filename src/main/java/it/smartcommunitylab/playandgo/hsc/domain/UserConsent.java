package it.smartcommunitylab.playandgo.hsc.domain;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="consent")
public class UserConsent {
    @Id
    private String id;
    
    @Indexed
    private String email;
    
    private boolean privacy;
    private boolean termOfConditions;
    private Date date;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public boolean isPrivacy() {
        return privacy;
    }
    public void setPrivacy(boolean privacy) {
        this.privacy = privacy;
    }
    public boolean isTermOfConditions() {
        return termOfConditions;
    }
    public void setTermOfConditions(boolean termOfConditions) {
        this.termOfConditions = termOfConditions;
    }
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
}
