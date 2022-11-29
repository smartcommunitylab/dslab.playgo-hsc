package it.smartcommunitylab.playandgo.hsc.service;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.smartcommunitylab.playandgo.hsc.domain.Avatar;
import it.smartcommunitylab.playandgo.hsc.domain.Image;
import it.smartcommunitylab.playandgo.hsc.error.DataException;
import it.smartcommunitylab.playandgo.hsc.error.HSCError;
import it.smartcommunitylab.playandgo.hsc.repository.AvatarRepository;
import it.smartcommunitylab.playandgo.hsc.util.ImageUtils;

@Service
public class AvatarService {
	private static transient final Logger logger = LoggerFactory.getLogger(AvatarService.class);
	
	private final static int DIMENSION = 640;
	private final static int DIMENSION_SMALL = 160;
	
	@Autowired
	AvatarRepository avatarRepository;
	
	@Autowired
	StorageService storageService;
	
	public Avatar getTeamAvatar(String teamId) {
		return avatarRepository.findByTeamId(teamId);
	}
	
	public Image getTeamSmallAvatar(String teamId) {
		Avatar avatar = avatarRepository.findByTeamId(teamId);
		if(avatar != null) {
			Image image = new Image();
			image.setContentType(avatar.getContentType());
			image.setUrl(avatar.getAvatarSmallUrl());
			return image;
		}
		return null;
	}
	
	public Avatar uploadTeamAvatar(String teamId, MultipartFile data) throws HSCError {
		if (data.getSize() > 10 * 1024 * 1024) {
			logger.warn("Image too big.");
			throw new DataException("image too big");
		}
		MediaType mediaType = MediaType.parseMediaType(data.getContentType());
		if (!mediaType.isCompatibleWith(MediaType.IMAGE_GIF) && !mediaType.isCompatibleWith(MediaType.IMAGE_JPEG) && !mediaType.isCompatibleWith(MediaType.IMAGE_PNG)) {
			logger.warn("Image format not supported");
			throw new DataException("Image format not supported");
		}
		Avatar avatar = getTeamAvatar(teamId);
		if(avatar == null) {
			avatar = new Avatar();
			avatar.setTeamId(teamId);
		}
		try {
			BufferedImage bs = ImageIO.read(data.getInputStream());
			byte cb[] = ImageUtils.compressImage(bs, data.getContentType(), DIMENSION);
			byte cbs[] = ImageUtils.compressImage(bs, data.getContentType(), DIMENSION_SMALL);
			String avatarImage = "avatar-" + teamId;
			String avatarSmallImage = "avatar-small-" + teamId;
			String avatarUrl = storageService.uploadImage(avatarImage, data.getContentType(), cb);
			avatar.setAvatarUrl(avatarUrl);
			String avatarSmallUrl = storageService.uploadImage(avatarSmallImage, data.getContentType(), cbs);
			avatar.setAvatarSmallUrl(avatarSmallUrl);
			avatar.setContentType(data.getContentType());
			avatarRepository.save(avatar);
		} catch (Exception e) {
			logger.warn("Error storing image:" + e.getMessage());
			throw new DataException("Error storing image");
		}
		return avatar;
	}
	
	public void deleteAvatar(String teamId) throws Exception {
		Avatar avatar = avatarRepository.findByTeamId(teamId);
		if(avatar != null) {
			String avatarImage = "avatar-" + teamId;
			String avatarSmallImage = "avatar-small-" + teamId;
			storageService.deleteImage(avatarImage);
			storageService.deleteImage(avatarSmallImage);
			avatarRepository.delete(avatar);
		}
	}
	
}
