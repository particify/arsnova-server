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

package de.thm.arsnova.util;

import static de.thm.arsnova.util.ImageUtils.IMAGE_PREFIX_MIDDLE;
import static de.thm.arsnova.util.ImageUtils.IMAGE_PREFIX_START;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import de.thm.arsnova.config.AppConfig;
import de.thm.arsnova.config.TestAppConfig;
import de.thm.arsnova.config.TestPersistanceConfig;
import de.thm.arsnova.config.TestSecurityConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
		AppConfig.class,
		TestAppConfig.class,
		TestPersistanceConfig.class,
		TestSecurityConfig.class})
@ActiveProfiles("test")
public class ImageUtilsTest {

  private ImageUtils imageUtils = new ImageUtils();

  @Test
  public void testNullIsNoValidBase64String() {
    assertFalse("\"null\" is no valid Base64 String.", imageUtils.isBase64EncodedImage(null));
  }

  @Test
  public void testEmptyStringIsNoValidBase64String() {
    assertFalse("The empty String is no valid Base64 String.", imageUtils.isBase64EncodedImage(""));
  }

  @Test
  public void testWrongStringIsNoValidBase64String() {
    final String[] fakeStrings = new String[] {
      "data:picture/png;base64,IMAGE-DATA",
      "data:image/png;base63,IMAGE-DATA"
    };

    for (final String fakeString : fakeStrings) {
      assertFalse(
        String.format("The String %s is not a valid Base64 String.", fakeString),
        imageUtils.isBase64EncodedImage(fakeString)
      );
    }
  }

  @Test
  public void testValidBase64String() {
    final String imageString = String.format("%spng%sIMAGE-DATA", IMAGE_PREFIX_START, IMAGE_PREFIX_MIDDLE);
    assertTrue(imageUtils.isBase64EncodedImage(imageString));
  }

  @Test
  public void testImageInfoExtraction() {
    final String extension = "png";
    final String imageData = "IMAGE-DATA";
    final String imageString = String.format("%s%s%s%s", IMAGE_PREFIX_START,
      extension, IMAGE_PREFIX_MIDDLE, imageData);

    final String[] imageInfo = imageUtils.extractImageInfo(imageString);
    assertNotNull(imageInfo);

    assertEquals("Extracted information doesn't match its specification.", 2, imageInfo.length);

    assertEquals("Extracted extension is invalid.", extension, imageInfo[0]);
    assertEquals("Extracted Base64-Image String is invalid.", imageData, imageInfo[1]);
  }

}
