package it.smartcommunitylab.playandgo.hsc.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.hsc.domain.PlayerTeam;

@Repository
public interface PlayerTeamRepository extends MongoRepository<PlayerTeam, String>{
	
	public List<PlayerTeam> findByInitiativeIdAndOwner(String initiativeId, String owner);
	public List<PlayerTeam> findByInitiativeId(String initiativeId);
	
	@Query("{'customData.name': {$regex: '^?0$', $options:'i'}}")
	public List<PlayerTeam> findByNickname(String name);
	
	public List<PlayerTeam> findByOwner(String owner);

}
