/** 
* ===License Header===
*
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
* ===License Header===
*/
package org.bigbluebutton.deskshare.client;

import java.awt.Point;

import org.bigbluebutton.deskshare.client.net.CursorMessage;
import org.bigbluebutton.deskshare.client.net.NetworkStreamSender;

public class MouseLocationListenerImp implements MouseLocationListener {

	private final NetworkStreamSender sender;
	private final String room;
	
	public MouseLocationListenerImp(NetworkStreamSender sender, String room) {
		this.sender = sender;
		this.room = room;
	}
	
	@Override
	public void onMouseLocationUpdate(Point loc) {
		CursorMessage msg = new CursorMessage(loc, room);
		sender.send(msg);
	}

}
