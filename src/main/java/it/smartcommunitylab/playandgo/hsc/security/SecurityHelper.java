/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.hsc.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * @author raman
 *
 */
@Service
public class SecurityHelper {
	static Log logger = LogFactory.getLog(SecurityHelper.class);
    
	@Value("${spring.security.oauth2.client.registration.oauthprovider.client-id}")
    private String jwtAudience;
	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;
	@Autowired
	private JwtDecoder decoder;
	
	public String getCurrentToken() {
		Jwt principal = getJwt();
		return principal.getTokenValue();
	}


	public String getCurrentSubject() {
		Jwt principal = getJwt();
		String subject = principal.getSubject();
		return subject;
	}
	
	public String getCurrentPreferredUsername() {
		Jwt principal = getJwt();
		String subject = principal.getClaimAsString("preferred_username");
		return subject;
	}
	
	public boolean checkAPIRole() {
		Jwt principal = getJwt();
		return principal.getAudience().contains(jwtAudience);
	}

	private Jwt getJwt() throws SecurityException {
		if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken) {
			return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		} else {
			OAuth2AuthenticationToken token  = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
			OAuth2AuthorizedClient client = authorizedClientService
				      .loadAuthorizedClient(
				        token.getAuthorizedClientRegistrationId(), 
				          token.getName());
			if (client == null) {
				throw new SecurityException();
			}
			OAuth2AccessToken t = client.getAccessToken();
			return decoder.decode(t.getTokenValue());
		}

	}
	
}
