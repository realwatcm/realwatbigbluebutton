/* BigBlueButton - http://www.bigbluebutton.org
 * 
 * 
 * Copyright (c) 2008-2009 by respective authors (see below). All rights reserved.
 * 
 * BigBlueButton is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 3 of the License, or (at your option) any later 
 * version. 
 * 
 * BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with BigBlueButton; if not, If not, see <http://www.gnu.org/licenses/>.
 *
 * Author: Richard Alam <ritzalam@gmail.com>
 * 		   DJP <DJP@architectes.org>
 * 
 * @version $Id: $
 */
package org.bigbluebutton.presentation;

import java.io.File;
import org.bigbluebutton.presentation.ConversionUpdateMessage.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SupportedDocumentFilter {
	private static Logger log = LoggerFactory.getLogger(SupportedDocumentFilter.class);
	private ConversionProgressNotifier notifier;
	
	public boolean isSupported(UploadedPresentation pres) {
		File presentationFile = pres.getUploadedFile();
		
		/* Get file extension - Perhaps try to rely on a more accurate method than an extension type ? */
		int fileExtIndex = presentationFile.getName().lastIndexOf('.') + 1;
		String ext = presentationFile.getName().toLowerCase().substring(fileExtIndex);
		boolean supported = SupportedFileTypes.isFileSupported(ext);
		notifyProgressListener(supported, pres);
		if (supported) {
			log.info("Received supported file " + pres.getUploadedFile().getAbsolutePath());
			pres.setFileType(ext);
		}
		return supported;
	}
	
	private void notifyProgressListener(boolean supported, UploadedPresentation pres) {
		MessageBuilder builder = new ConversionUpdateMessage.MessageBuilder(pres);
						
		if (supported) {
			builder.messageKey(ConversionMessageConstants.SUPPORTED_DOCUMENT_KEY);
		} else {
			builder.messageKey(ConversionMessageConstants.UNSUPPORTED_DOCUMENT_KEY);
		}
		
		if (notifier != null) {
			notifier.sendConversionProgress(builder.build().getMessage());
		} else {
			log.warn("ConversionProgressNotifier has not been set!");
		}
	}
	
	public void setConversionProgressNotifier(ConversionProgressNotifier notifier) {
		this.notifier = notifier;
	}
}
