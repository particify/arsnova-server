/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2017 The ARSnova Team
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

import de.thm.arsnova.entities.Answer;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Util class for image operations.
 *
 * @author Daniel Vogel (daniel.vogel@mni.thm.de)
 * @author Jan Sladek (jan.sladek@mni.thm.de)
 */
@Component("imageUtils")
public class ImageUtils {

	// Or whatever size you want to read in at a time.
	static final int CHUNK_SIZE = 4096;

	/** Base64-Mimetype-Prefix start */
	static final String IMAGE_PREFIX_START = "data:image/";

	/** Base64-Mimetype-Prefix middle part */
	static final String IMAGE_PREFIX_MIDDLE = ";base64,";

	/* default value is 200 pixel in width, set the value in the configuration file */
	static final int THUMB_WIDTH_DEFAULT = 200;
	/* default value is 200 pixel in height, set the value in the configuration file */
	static final int THUMB_HEIGHT_DEFAULT = 200;

	public static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

	@Value("${imageupload.thumbnail.width}")
	private int thumbWidth = THUMB_WIDTH_DEFAULT;

	@Value("${imageupload.thumbnail.height}")
	private int thumbHeight = THUMB_HEIGHT_DEFAULT;

	/**
	 * Converts an image to an Base64 String.
	 *
	 * @param  imageUrl The image url as a {@link String}
	 * @return The Base64 {@link String} of the image on success, otherwise <code>null</code>.
	 */
	public String encodeImageToString(final String imageUrl) {

		final String[] urlParts = imageUrl.split("\\.");

		// get format
		//
		// The format is read dynamically. We have to take control
		// in the frontend that no unsupported formats are transmitted!
		if (urlParts.length > 0) {
			final String extension = urlParts[urlParts.length - 1];

			return "data:image/" + extension + ";base64," + Base64.encodeBase64String(convertFileToByteArray(imageUrl));
		}

		return null;
	}

	/**
	 * Checks if a {@link String} starts with the Base64-Mimetype prefix.
	 *
	 * @param maybeImage The Image as a base64 encoded {@link String}
	 * @return true if the string is a potentially a base 64 encoded image.
	 */
	boolean isBase64EncodedImage(String maybeImage) {
		return extractImageInfo(maybeImage) != null;
	}

	/**
	 * Extracts information(extension and the raw-image) from a {@link String}
	 * representing a base64-encoded image and returns it as a two-dimensional
	 * {@link String}-array, or null if the passed in {@link String} is not a
	 * valid base64-encoded image.
	 *
	 * @param maybeImage
	 *            a {@link String} representing a base64-encoded image.
	 * @return two-dimensional {@link String}-array containing the information
	 *         "extension" and the "raw-image-{@link String}"
	 */
	String[] extractImageInfo(final String maybeImage) {
		if (maybeImage == null) {
			return null;
		} else if (maybeImage.isEmpty()) {
			return null;
		} else {
			if (!maybeImage.startsWith(IMAGE_PREFIX_START)) {
				return null;
			} else {
				final int extensionStartIndex = IMAGE_PREFIX_START.length();
				final int extensionEndIndex = maybeImage.indexOf(IMAGE_PREFIX_MIDDLE);
				if (extensionEndIndex < 0) {
					return null;
				}

				final String imageWithoutPrefix = maybeImage.substring(extensionEndIndex);

				if (!imageWithoutPrefix.startsWith(IMAGE_PREFIX_MIDDLE)) {
					return null;
				} else {
					final String[] imageInfo = new String[2];
					final String extension = maybeImage.substring(extensionStartIndex, extensionEndIndex);
					final String imageString = imageWithoutPrefix.substring(IMAGE_PREFIX_MIDDLE.length());

					imageInfo[0] = extension;
					imageInfo[1] = imageString;

					return imageInfo;
				}
			}
		}
	}

	/**
	 * Rescales an image represented by a Base64-encoded {@link String}
	 *
	 * @param originalImageString
	 *            The original image represented by a Base64-encoded
	 *            {@link String}
	 * @param width
	 *            the new width
	 * @param height
	 *            the new height
	 * @return The rescaled Image as Base64-encoded {@link String}, returns null
	 *         if the passed-on image isn't in a valid format (a Base64-Image).
	 */
	String createCover(String originalImageString, final int width, final int height) {
		if (!isBase64EncodedImage(originalImageString)) {
			return null;
		} else {
			final String[] imgInfo = extractImageInfo(originalImageString);

			// imgInfo isn't null and contains two fields, this is checked by "isBase64EncodedImage"-Method
			final String extension = imgInfo[0];
			final String base64String = imgInfo[1];

			byte[] imageData = Base64.decodeBase64(base64String);
			try {
				BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
				BufferedImage newImage = new BufferedImage(width, height, originalImage.getType());
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

				result.append(Base64.encodeBase64String(output.toByteArray()));

				return result.toString();
			} catch (IOException e) {
				logger.error(e.getLocalizedMessage());
				return null;
			}
		}
	}

	/**
	 * Generates a thumbnail image in the {@link Answer}, if none is present.
	 *
	 * @param answer
	 *            the {@link Answer} where the thumbnail should be added.
	 * @return true if the thumbnail image didn't exist before calling this
	 *         method, false otherwise
	 */
	public boolean generateThumbnailImage(Answer answer) {
		if (!isBase64EncodedImage(answer.getAnswerThumbnailImage())) {
			final String thumbImage = createCover(answer.getAnswerImage(), thumbWidth, thumbHeight);
			answer.setAnswerThumbnailImage(thumbImage);
			return true;
		}
		return false;
	}

	/**
	 * Gets the bytestream of an image url.
	 *
	 * @param  imageUrl The image url as a {@link String}
	 * @return The <code>byte[]</code> of the image on success, otherwise <code>null</code>.
	 */
	byte[] convertFileToByteArray(final String imageUrl) {
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

		} catch (IOException e) {
			logger.error(e.getLocalizedMessage());
		}

		return null;
	}
}
