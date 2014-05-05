/*
 * Copyright (C) 2014 THM webMedia
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

package de.thm.arsnova;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

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
	public static final Logger LOGGER = LoggerFactory.getLogger(ImageUtils.class);

	/**
	 * Converts an image to an Base64 String.
	 * 
	 * @param  imageUrl The image url as a {@link String}
	 * @return The Base64 {@link String} of the image on success, otherwise <code>null</code>.
	 */
	public static String encodeImageToString(String imageUrl) {
		
		String[]      urlParts = imageUrl.split("\\.");
		StringBuilder result   = new StringBuilder();
		
		// get format
	    //
	    // The format is read dynamically. We have to take control
	    // in the frontend that no unsupported formats are transmitted!
	    if ( urlParts.length > 0 ) {

			String extension = urlParts[urlParts.length-1];

	    	result.append("data:image/" + extension + ";base64,");
	    	result.append(Base64.encodeBase64String(convertFileToByteArray(imageUrl)));
	    	
			return result.toString();
	    }
	    
		return null;
	}
	
	/**
	 * Gets the bytestream of an image url.
	 * s
	 * @param  imageUrl The image url as a {@link String}
	 * @return The <code>byte[]</code> of the image on success, otherwise <code>null</code>.
	 */
	public static byte[] convertImageToByteArray(String imageUrl, String extension) {

		try {
			URL 				  url   = new URL(imageUrl);
			BufferedImage 		  image = ImageIO.read(url);
		    ByteArrayOutputStream baos  = new ByteArrayOutputStream();

		    ImageIO.write(image, extension, baos);
		    
		    baos.flush();
		    baos.close();
		    return baos.toByteArray();
		    
		} catch (MalformedURLException e) {
			LOGGER.error(e.getLocalizedMessage());
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage());
		}

	    return null;
	}
	
	/**
	 * Gets the bytestream of an image url.
	 * s
	 * @param  imageUrl The image url as a {@link String}
	 * @return The <code>byte[]</code> of the image on success, otherwise <code>null</code>.
	 */
	public static byte[] convertFileToByteArray(String imageUrl) {

		try {
			URL 				  url   = new URL(imageUrl);
		    ByteArrayOutputStream baos  = new ByteArrayOutputStream();
		    
		    InputStream is = url.openStream();
		    byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
		    int n;

		    while ( (n = is.read(byteChunk)) > 0 ) {
		    	baos.write(byteChunk, 0, n);
		    }

		    baos.flush();
		    baos.close();
		    
		    return baos.toByteArray();
		    
		} catch (MalformedURLException e) {
			LOGGER.error(e.getLocalizedMessage());
		} catch (IOException e) {
			LOGGER.error(e.getLocalizedMessage());
		}

	    return null;
	}
}

