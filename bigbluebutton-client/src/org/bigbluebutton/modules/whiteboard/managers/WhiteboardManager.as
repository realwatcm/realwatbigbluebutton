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
package org.bigbluebutton.modules.whiteboard.managers
{
	import com.asfusion.mate.events.Dispatcher;
	
	import flash.events.TimerEvent;
	import flash.utils.Timer;
	
	import org.bigbluebutton.main.events.AddUIComponentToMainCanvas;
	import org.bigbluebutton.modules.present.api.PresentationAPI;
	import org.bigbluebutton.modules.present.events.AddButtonToPresentationEvent;
	import org.bigbluebutton.modules.present.events.AddOverlayCanvasEvent;
	import org.bigbluebutton.modules.whiteboard.events.WhiteboardButtonEvent;
	import org.bigbluebutton.modules.whiteboard.views.WhiteboardButton;
	import org.bigbluebutton.modules.whiteboard.views.WhiteboardCanvas;
	import org.bigbluebutton.modules.whiteboard.views.WhiteboardToolbar;
	
	public class WhiteboardManager
	{
		private var globalDispatcher:Dispatcher;
		private var highlighterCanvas:WhiteboardCanvas;
		private var highlighterToolbar:WhiteboardToolbar;
		private var whiteboardButton:WhiteboardButton;
		
		public function WhiteboardManager()
		{
			globalDispatcher = new Dispatcher();
		}
		
		public function handleStartModuleEvent():void{	
			if (highlighterCanvas != null) return;
			highlighterCanvas = new WhiteboardCanvas();
			if (highlighterToolbar != null) return;
			highlighterToolbar = new WhiteboardToolbar();
			highlighterToolbar.canvas = highlighterCanvas;
			if (whiteboardButton != null) return;
			whiteboardButton = new WhiteboardButton();
			
			//Necessary now because of module loading race conditions
			var t:Timer = new Timer(2000, 1);
			t.addEventListener(TimerEvent.TIMER, addHighlighterCanvas);
			t.addEventListener(TimerEvent.TIMER, addHighlighterToolbar);
			t.start();
		}
		
		private function addHighlighterCanvas(e:TimerEvent):void{
			PresentationAPI.getInstance().addOverlayCanvas(highlighterCanvas);
		}
		
		private function addHighlighterToolbar(e:TimerEvent):void{
			PresentationAPI.getInstance().addButtonToToolbar(whiteboardButton);
		}
		
		public function positionToolbar(e:WhiteboardButtonEvent):void{
			var addUIEvent:AddUIComponentToMainCanvas = new AddUIComponentToMainCanvas(AddUIComponentToMainCanvas.ADD_COMPONENT);
			addUIEvent.component = highlighterToolbar;
			globalDispatcher.dispatchEvent(addUIEvent);
			highlighterToolbar.positionToolbar(e.window);
		}

	}
}