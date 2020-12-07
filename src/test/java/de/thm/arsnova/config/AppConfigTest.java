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

package de.thm.arsnova.config;

import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Arrays;

import de.thm.arsnova.config.properties.SecurityProperties;
import java.util.List;

import static org.junit.Assert.assertEquals;

import net.particify.arsnova.connector.client.ConnectorClient;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class})
@ActiveProfiles("test")
public class AppConfigTest extends AbstractJUnit4SpringContextTests {
	@Autowired
	private SecurityProperties securityProperties;

	@Autowired(required = false)
	private ConnectorClient connectorClient;

	private List<String> adminAccounts;

	@Test
	public void testShouldNotLoadConnectorClientByDefault() {
		assertNull(connectorClient);
	}

	@Test
	public void testShouldUseAdminAccountFromTestPropertiesFile() {
		final List<String> expected = Collections.singletonList("TestAdmin");
		final List<String> actual = securityProperties.getAdminAccounts().stream()
				.map(adminAccount -> adminAccount.getLoginId()).collect(Collectors.toList());

		assertEquals("Configuration did not load correct property file", expected, actual);
	}
}
