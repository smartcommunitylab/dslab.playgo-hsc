package it.smartcommunitylab.playandgo.hsc.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.playandgo.hsc.domain.UserConsent;
import it.smartcommunitylab.playandgo.hsc.service.UserConsentService;

@RestController
public class UserController {
	
	@Autowired
	UserConsentService userConsentService;
	
	@GetMapping("/api/user/consent")
	public ResponseEntity<UserConsent> getUserConsent() {
		return ResponseEntity.ok(userConsentService.getUserConsent());
	}
	
	@PutMapping("/api/user/consent")
	public ResponseEntity<UserConsent> updateConsent(@RequestParam boolean privacy, @RequestParam boolean termOfConditions) {
		return ResponseEntity.ok(userConsentService.updateConsent(privacy, termOfConditions));
	}
}
