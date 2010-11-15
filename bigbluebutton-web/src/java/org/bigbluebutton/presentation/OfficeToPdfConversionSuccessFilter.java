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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfficeToPdfConversionSuccessFilter {
	private static Logger log = LoggerFactory.getLogger(OfficeToPdfConversionSuccessFilter.class);
	private ConversionProgressNotifier notifier;
	
	public boolean didConversionSucceed(UploadedPresentation pres) {
		notifyProgressListener(pres);
		return pres.isLastStepSuccessful();
	}

	private void notifyProgressListener(UploadedPresentation pres) {
		Map<String, Object> msg = new HashMap<String, Object>();
		msg.put("conference", pres.getConference());
		msg.put("room", pres.getRoom());
		msg.put("returnCode", "CONVERT");
		msg.put("presentationName", pres.getName());
		
		if (pres.isLastStepSuccessful()) {
			log.info("Notifying of OFFICE_DOC_CONVERSION_SUCCESS for " + pres.getUploadedFile().getAbsolutePath());
			msg.put("message", "Office document successfully converted.");
			msg.put("messageKey", "OFFICE_DOC_CONVERSION_SUCCESS");
		} else {
			log.info("Notifying of OFFICE_DOC_CONVERSION_FAILED for " + pres.getUploadedFile().getAbsolutePath());
			msg.put("message", "Failed to convert Office document.");
			msg.put("messageKey", "OFFICE_DOC_CONVERSION_FAILED");
		}
		
		sendNotification(msg);
	}
	
	private void sendNotification(Map<String, Object> msg) {
		if (notifier != null) {
			notifier.sendConversionProgress(msg);	
		} else {
			log.warn("ConversionProgressNotifier has not been set!.");
		}
	}
		
	public void setConversionProgressNotifier(ConversionProgressNotifier notifier) {
		this.notifier = notifier;
	}
}
