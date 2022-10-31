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

package net.particify.arsnova.core.management;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Resource;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class VersionInfoContributor implements InfoContributor {
  private Map<String, Object> infoDetails;

  @Override
  public void contribute(final Info.Builder builder) {
    builder.withDetails(infoDetails);
  }

  @Resource(name = "versionInfoProperties")
  public void setVersionInfoProperties(final Properties versionInfoProperties) {
    infoDetails = new HashMap<>();
    final Map<String, Object> version = new HashMap<>();

    version.put("string", versionInfoProperties.getProperty("version.string"));
    version.put("buildTime", versionInfoProperties.getProperty("version.build-time"));
    version.put("gitCommitId", versionInfoProperties.getProperty("version.git.commit-id"));
    version.put("gitDirty", Boolean.parseBoolean(versionInfoProperties.getProperty("version.git.dirty")));

    infoDetails.put("productName", "arsnova-backend");
    infoDetails.put("version", version);
  }

  public Map<String, Object> getInfoDetails() {
    return infoDetails;
  }
}
