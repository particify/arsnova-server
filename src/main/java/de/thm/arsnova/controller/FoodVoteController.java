/*
 * Copyright (C) 2012 THM webMedia
 *
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.entities.FoodVote;
import de.thm.arsnova.services.IFoodService;

@Controller
public class FoodVoteController extends AbstractController {

	public static final Logger LOGGER = LoggerFactory
			.getLogger(FoodVoteController.class);

	@Autowired
	private IFoodService foodService;

	@RequestMapping(value = "/canteen/menu/vote", method = RequestMethod.POST)
	public final void setFoodVote(
			@RequestBody final Object menu,
			final HttpServletResponse response
		) {
		String menustring = JSONObject.fromObject(menu).getString("menu");
		foodService.vote(menustring);
	}
	
	@RequestMapping(value = "/canteen/menu/vote", method = RequestMethod.GET)
	@ResponseBody
	public final List<FoodVote> getFoodVote() {
		return foodService.getFoodVote();
	}

	@RequestMapping(value = "/canteen/menu/vote/count", method = RequestMethod.GET)
	@ResponseBody
	public final int getFoodVoteCount() {
		return foodService.getFoodVoteCount();
	}
	
	

}
