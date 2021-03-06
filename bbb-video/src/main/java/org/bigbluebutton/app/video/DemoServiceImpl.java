/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
*/
package org.bigbluebutton.app.video;

import java.util.HashMap;
import java.util.Map;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

public class DemoServiceImpl implements IDemoService {
	
	private static Logger log = Red5LoggerFactory.getLogger(DemoServiceImpl.class, "video");
	
	/**
     * Getter for property 'listOfAvailableFLVs'.
     *
     * @return Value for property 'listOfAvailableFLVs'.
     */
    public Map<String, Map<String, Object>> getListOfAvailableFLVs() {
    	log.debug("getListOfAvailableFLVs empty");
		return new HashMap<String, Map<String, Object>>(1);
	}

    public Map<String, Map<String, Object>> getListOfAvailableFLVs(String string) {
    	log.debug("getListOfAvailableFLVs, Got a string: {}", string);
    	return getListOfAvailableFLVs();
    }

}

