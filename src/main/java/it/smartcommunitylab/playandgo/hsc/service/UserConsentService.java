package it.smartcommunitylab.playandgo.hsc.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.playandgo.hsc.domain.UserConsent;
import it.smartcommunitylab.playandgo.hsc.repository.UserConsentRepository;
import it.smartcommunitylab.playandgo.hsc.security.SecurityHelper;

@Service
public class UserConsentService {
	@Autowired
	UserConsentRepository userConsentRepository;
	
	@Autowired
	private SecurityHelper securityHelper;
	
	public boolean existConsent() throws SecurityException {
		String email = securityHelper.getCurrentPreferredUsername();
		UserConsent consent = userConsentRepository.findByEmail(email);
		if((consent != null) && consent.isPrivacy()) {
			return true;
		}			
		return false;
	}
	
	public UserConsent getUserConsent() {
		String email = securityHelper.getCurrentPreferredUsername();
		return  userConsentRepository.findByEmail(email);
	}
	
	public UserConsent updateConsent(boolean privacy) {
		String email = securityHelper.getCurrentPreferredUsername();
		UserConsent consent = userConsentRepository.findByEmail(email);
		if(consent != null) {
			return consent;
		}
		consent = new UserConsent();
		consent.setEmail(email);
		consent.setDate(new Date());
		consent.setPrivacy(privacy);
		userConsentRepository.save(consent);
		return consent;
	}
	
	

}
