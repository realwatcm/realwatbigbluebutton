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

package org.bigbluebutton.modules.phone.managers
{
	import flash.events.IEventDispatcher;
	
	import org.bigbluebutton.common.LogUtil;
	import org.bigbluebutton.modules.phone.events.CallConnectedEvent;
	import org.bigbluebutton.modules.phone.events.JoinVoiceConferenceEvent;
	
	public class PhoneManager {
		
		private var connectionManager:ConnectionManager;
		private var streamManager:StreamManager;
		private var onCall:Boolean = false;
		private var attributes:Object;
		
		public function PhoneManager() {
			connectionManager = new ConnectionManager();
			streamManager = new StreamManager();
		}

		public function setModuleAttributes(attributes:Object):void {
			this.attributes = attributes;
			LogUtil.debug("Attributes Set... webvoiceconf:" + attributes.webvoiceconf);
		}
				
		private function setupMic(useMic:Boolean):void {
			if (useMic)
				streamManager.initMicrophone();
			else
				streamManager.initWithNoMicrophone();
		}
		
		private function setupConnection():void {
			streamManager.setConnection(connectionManager.getConnection());
		}
		
		public function join(e:JoinVoiceConferenceEvent):void {
			setupMic(e.useMicrophone);
			var uid:String = String( Math.floor( new Date().getTime() ) );
			connectionManager.connect(uid, attributes.username, attributes.room, attributes.uri);
		}
				
		public function dialConference():void {
			LogUtil.debug("Dialing...." + attributes.webvoiceconf);
			connectionManager.doCall(attributes.webvoiceconf);
		}
		
		public function callConnected(event:CallConnectedEvent):void {
			LogUtil.debug("Call connected...");
			setupConnection();
			streamManager.callConnected(event.playStreamName, event.publishStreamName, event.codec);
			onCall = true;
		}
		
		public function hangup():void {
			LogUtil.debug("PhoneManager hangup");
			if (onCall) {
				streamManager.stopStreams();
				connectionManager.doHangUp();
				onCall = false;
			}			
		}
	}
}
