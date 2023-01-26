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

package it.smartcommunitylab.playandgo.hsc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.playandgo.hsc.domain.Initiative;
import it.smartcommunitylab.playandgo.hsc.service.PlayerTeamService;

/**
 * @author raman
 *
 */
@Controller
public class WebController {

	@Autowired
	private PlayerTeamService teamService;
    @Value("${spring.security.oauth2.client.provider.oauthprovider.authorization-uri}")
    private String authUri;
    @Value("${spring.security.oauth2.client.registration.oauthprovider.client-id}")
    private String clientId;

	@GetMapping("/web/{type}/{initiative}")
	public 
	ModelAndView webBoard(@PathVariable String type, @PathVariable String initiative) {
		ModelAndView model = new ModelAndView("web/"+ type);
		Initiative obj = teamService.getInitiative(initiative);
		model.addObject("initiative", obj);
		return model;
	}

	@GetMapping("/web")
	public 
	ModelAndView webMgmtList() {
		ModelAndView model = new ModelAndView("web/list");
		model.addObject("authEndpoint", authUri);
		model.addObject("clientId", clientId);
		return model;
	}

	@GetMapping("/web/{type}/{initiative}/mgmt")
	public 
	ModelAndView webMgmt(@PathVariable String type, @PathVariable String initiative) {
		ModelAndView model = new ModelAndView("web/mgmt"+type);
		Initiative obj = teamService.getInitiative(initiative);
		model.addObject("authEndpoint", authUri);
		model.addObject("clientId", clientId);
		model.addObject("initiativeId", obj.getInitiativeId());
		model.addObject("minTeamSize", obj.getMinTeamSize());
		model.addObject("campaignName", obj.getCampaign().getName());
		return model;
	}
	
	@GetMapping("/web/team/mgmt")
	public 
	ModelAndView webTeamMgmt() {
		ModelAndView model = new ModelAndView("web/teammgmthsc");
		model.addObject("authEndpoint", authUri);
		model.addObject("clientId", clientId);
		return model;
	}
	
}
