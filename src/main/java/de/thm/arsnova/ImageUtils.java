/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2015 The ARSnova Team
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
package de.thm.arsnova;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class for image operations.
 *
 * @author Daniel Vogel (daniel.vogel@mni.thm.de)
 *
 */
public class ImageUtils {

	// Or whatever size you want to read in at a time.
	private static final int CHUNK_SIZE = 4096;
	
	public static final Pattern BASE64_IMAGE_PREFIX_PATTERN = Pattern.compile("data:image/(.*);base64,(.*)");
	

	private ImageUtils() {
	}

	public static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

	/**
	 * Converts an image to an Base64 String.
	 *
	 * @param  imageUrl The image url as a {@link String}
	 * @return The Base64 {@link String} of the image on success, otherwise <code>null</code>.
	 */
	public static String encodeImageToString(final String imageUrl) {

		final String[] urlParts = imageUrl.split("\\.");
		final StringBuilder result   = new StringBuilder();

		// get format
		//
		// The format is read dynamically. We have to take control
		// in the frontend that no unsupported formats are transmitted!
		if (urlParts.length > 0) {
			final String extension = urlParts[urlParts.length - 1];

			result.append("data:image/" + extension + ";base64,");
			result.append(Base64.encodeBase64String(convertFileToByteArray(imageUrl)));

			return result.toString();
		}

		return null;
	}
	
	/**
	 * Checks if a String starts with the base 64 prefix.
	 * 
	 * @param maybeImage The Image as a base64 encoded {@link String} 
	 * @return true if the string is a potentially a base 64 encoded image.
	 */
	public static boolean isBase64EncodedImage(String maybeImage) {
		if (maybeImage == null) {
			return false;
		}
		else if (maybeImage.isEmpty()) {
			return false;
		}
		else
			return BASE64_IMAGE_PREFIX_PATTERN.matcher(maybeImage).matches();
	}
	
	public static String rescaleImage(String originalImageString, final int width, final int height) {
		if (!isBase64EncodedImage(originalImageString)) return null;
		else {
			Matcher imageMatcher = BASE64_IMAGE_PREFIX_PATTERN.matcher(originalImageString);
			if (!imageMatcher.find()) {
				// shouldn't ever happen, because the regex is already checked.
				return null;
			}
			String extension = imageMatcher.group(1);
			String base64String = imageMatcher.group(2);
			
			byte[] imageData = Base64.decodeBase64(base64String);
			try {
				BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
				BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = newImage.createGraphics();
				
				final double ratio = ((double) originalImage.getWidth()) / ((double) originalImage.getHeight());
				
				int x = 0, y = 0, w = width, h = height;
				if (originalImage.getWidth() > originalImage.getHeight()) {
					final int newWidth = (int) Math.round((float) height * ratio);
					x = -(newWidth - width) >> 1;
					w = newWidth;
				} else if (originalImage.getWidth() < originalImage.getHeight()) {
					final int newHeight = (int) Math.round((float) width / ratio);
					y = -(newHeight - height) >> 1;
					h = newHeight;
				}
				g.drawImage(originalImage, x, y, w, h, null);
				g.dispose();
				
				StringBuilder result = new StringBuilder();
				result.append("data:image/");
				result.append(extension);
				result.append(";base64,");
				
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				ImageIO.write(newImage, extension, output);
				
				output.flush();
				output.close();
				
				return Base64.encodeBase64String(output.toByteArray());
			} catch (IOException e) {
				LOGGER.error(e.getLocalizedMessage());
				return null;
			}
		}
	}

	/**
	 * Gets the bytestream of an image url.
	 * s
	 * @param  imageUrl The image url as a {@link String}
	 * @return The <code>byte[]</code> of the image on success, otherwise <code>null</code>.
	 */
	public static byte[] convertImageToByteArray(final String imageUrl, final String extension) {

		try {
			final URL url = new URL(imageUrl);
			final BufferedImage image = ImageIO.read(url);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();

			ImageIO.write(image, extension, baos);

			baos.flush();
			baos.close();
			return baos.toByteArray();

		} catch (final MalformedURLException e) {
			LOGGER.error(e.getLocalizedMessage());
		} catch (final IOException e) {
			LOGGER.error(e.getLocalizedMessage());
		}

		return null;
	}

	/**
	 * Gets the bytestream of an image url.
	 *
	 * @param  imageUrl The image url as a {@link String}
	 * @return The <code>byte[]</code> of the image on success, otherwise <code>null</code>.
	 */
	public static byte[] convertFileToByteArray(final String imageUrl) {


		try {
			final URL url = new URL(imageUrl);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();

			final InputStream is = url.openStream();
			final byte[] byteChunk = new byte[CHUNK_SIZE];
			int n;

			while ((n = is.read(byteChunk)) > 0) {
				baos.write(byteChunk, 0, n);
			}

			baos.flush();
			baos.close();

			return baos.toByteArray();

		} catch (final MalformedURLException e) {
			LOGGER.error(e.getLocalizedMessage());
		} catch (final IOException e) {
			LOGGER.error(e.getLocalizedMessage());
		}

		return null;
	}
}
