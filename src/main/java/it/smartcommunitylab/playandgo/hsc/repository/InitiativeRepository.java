package it.smartcommunitylab.playandgo.hsc.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.hsc.domain.Initiative;

@Repository
public interface InitiativeRepository extends MongoRepository<Initiative, String> {

	@Query("{'campaign.territoryId': {$in: ?0}}")
	List<Initiative> findByTerritories(List<String> territories);
	@Query("{'initiativeId': {$in: ?0}}")
	List<Initiative> findByCampaignIds(List<String> territories);

}
