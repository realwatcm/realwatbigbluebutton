/** 
*
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
**/
package org.bigbluebutton.voiceconf.red5.media.transcoder;

import org.red5.app.sip.stream.RtpStreamSender;

public class TranscodedPcmAudioBuffer {

	private byte[] buffer;
	private int offset;
	private RtpStreamSender sender;
	
	TranscodedPcmAudioBuffer(byte[] data, int offset, RtpStreamSender sender) {
		buffer = data;
		this.offset = offset;
	}
		
	boolean copyData(byte[] data) {
		if (data.length > buffer.length - offset)
			return false;
		
		System.arraycopy(data, 0, buffer, offset, data.length);
		return true;
	}
	
	void sendData() {
		sender.sendTranscodedData();
	}

}
