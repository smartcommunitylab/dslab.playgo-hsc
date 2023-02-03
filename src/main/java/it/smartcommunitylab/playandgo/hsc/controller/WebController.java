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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import it.smartcommunitylab.playandgo.hsc.domain.Initiative;
import it.smartcommunitylab.playandgo.hsc.security.SecurityHelper;
import it.smartcommunitylab.playandgo.hsc.service.PlayerTeamService;
import it.smartcommunitylab.playandgo.hsc.service.UserConsentService;

/**
 * @author raman
 *
 */
@Controller
public class WebController {

	@Autowired
	private SecurityHelper helper;
	
	@Autowired
	private PlayerTeamService teamService;
	
	@Autowired
	UserConsentService userConsentService;

	@GetMapping("/")
	public 
	ModelAndView root() {
		return new ModelAndView("redirect:/web");
	}

	@GetMapping("/web")
	public 
	ModelAndView webMgmtList() {
		List<Initiative> list;
		try {
			list = teamService.getInitativesForManager();
			ModelAndView model = new ModelAndView(list.size() > 0 ? "redirect:/web/initiatives" : "redirect:/web/teams");
			return model;
		} catch (Exception e) {
			return new ModelAndView("redirect:/logout");
		}
	}
	@GetMapping("/web/consent")
	public 
	ModelAndView webConsent() {
		ModelAndView model = new ModelAndView("web/consentform");
		try {
			model.addObject("token", getToken());
		} catch (Exception e) {
			return new ModelAndView("redirect:/logout");
		}
		return model;
	}	
	@GetMapping("/web/teams")
	public 
	ModelAndView webTeamMgmt() {
		try {
			if(!userConsentService.existConsent()) {
				return new ModelAndView("redirect:/web/consent");
			}
			ModelAndView model = new ModelAndView("web/teammgmthsc");
			model.addObject("token", getToken());
			return model;
		} catch (Exception e) {
			return new ModelAndView("redirect:/logout");
		}
	}
	@GetMapping("/web/initiatives")
	public 
	ModelAndView weInitiativeMgmt() {
		try {
			if(!userConsentService.existConsent()) {
				return new ModelAndView("redirect:/web/consent");
			}
			ModelAndView model = new ModelAndView("web/list"); 
			model.addObject("token", getToken());
			return model;
		} catch (Exception e) {
			return new ModelAndView("redirect:/logout");
		}
	}
	@GetMapping("/web/{initiativeId}/mgmt")
	public 
	ModelAndView webMgmt(@PathVariable String initiativeId) {
		try {
			if(!userConsentService.existConsent()) {
				return new ModelAndView("redirect:/web/consent");
			}		
			ModelAndView model = new ModelAndView("web/mgmthsc");
			Initiative obj = teamService.getInitiative(initiativeId);
			model.addObject("token", getToken());
			model.addObject("initiative", obj);
			return model;
		} catch (Exception e) {
			return new ModelAndView("redirect:/logout");
		}
	}
	
//	@GetMapping("/web/logout")
//	public 
//	ModelAndView logout() {
//		return new ModelAndView("redirect:/");
//	}
	
	private String getToken() {
		return helper.getCurrentToken();
	}
	
}
