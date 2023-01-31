package it.smartcommunitylab.playandgo.hsc.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.hsc.domain.UserConsent;

@Repository
public interface UserConsentRepository extends MongoRepository<UserConsent, String> {
	
	public UserConsent findByEmail(String email);

}
