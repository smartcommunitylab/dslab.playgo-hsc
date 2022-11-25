package it.smartcommunitylab.playandgo.hsc.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.playandgo.hsc.domain.Avatar;

@Repository
public interface AvatarRepository extends MongoRepository<Avatar, String> {
	
	public Avatar findByTeamId(String teamId);

}
