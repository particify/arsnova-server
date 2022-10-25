/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.particify.arsnova.core.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import net.particify.arsnova.core.management.VersionInfoContributor;

/**
 * Default controller that handles requests which have not set a path.
 */
@Controller
public class WelcomeController extends AbstractController {
  @Autowired
  private VersionInfoContributor versionInfoContributor;

  @GetMapping("/")
  public View home() {
    return new RedirectView("/", false);
  }

  @GetMapping(value = "/", produces = "application/json")
  @ResponseBody
  public Map<String, Object> jsonHome() {
    return versionInfoContributor.getInfoDetails();
  }
}
