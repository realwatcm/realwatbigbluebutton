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

package org.bigbluebutton.modules.deskshare.managers
{
	import com.asfusion.mate.events.Dispatcher;
	
	import org.bigbluebutton.common.IBbbModuleWindow;
	import org.bigbluebutton.common.LogUtil;
	import org.bigbluebutton.main.events.CloseWindowEvent;
	import org.bigbluebutton.main.events.OpenWindowEvent;
	import org.bigbluebutton.modules.deskshare.services.DeskshareService;
	import org.bigbluebutton.modules.deskshare.view.components.DesktopViewWindow;
			
	public class ViewerWindowManager {		
		private var viewWindow:DesktopViewWindow;
		private var service:DeskshareService;
		private var isViewing:Boolean = false;
		private var globalDispatcher:Dispatcher;
		
		public function ViewerWindowManager(service:DeskshareService) {
			this.service = service;
			globalDispatcher = new Dispatcher();
		}
					
		public function stopViewing():void {
			if (isViewing) viewWindow.stopViewing();
		}
				
		public function handleStartedViewingEvent():void{
			LogUtil.debug("ViewerWindowManager handleStartedViewingEvent");
			service.sendStartedViewingNotification();
		}
						
		private function openWindow(window:IBbbModuleWindow):void{				
			var event:OpenWindowEvent = new OpenWindowEvent(OpenWindowEvent.OPEN_WINDOW_EVENT);
			event.window = window;
			globalDispatcher.dispatchEvent(event);
		}
			
		public function handleViewWindowCloseEvent():void {
			LogUtil.debug("ViewerWindowManager Received stop viewing command");				
			closeWindow(viewWindow);
			isViewing = false;	
		}
		
		private function closeWindow(window:IBbbModuleWindow):void {
			var event:CloseWindowEvent = new CloseWindowEvent(CloseWindowEvent.CLOSE_WINDOW_EVENT);
			event.window = window;
			globalDispatcher.dispatchEvent(event);
		}
			
		public function startViewing(room:String, videoWidth:Number, videoHeight:Number):void{
			LogUtil.debug("ViewerWindowManager::startViewing");
			viewWindow = new DesktopViewWindow();
			viewWindow.startVideo(service.getConnection(), room, videoWidth, videoHeight);
			
			openWindow(viewWindow);
			isViewing = true;
		}
	}
}