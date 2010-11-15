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
package org.bigbluebutton.modules.present.events
{
	import flash.events.Event;
	
	/**
	 * This class represents all the events that a presenter would send to the viewers. In other words, these are the events that are propagated
	 * across the server and to other clients, unlike all the other events, which are local.  
	 * @author Denis
	 * 
	 */	
	public class PresenterCommands extends Event
	{
		public static const GOTO_SLIDE:String = "GOTO_SLIDE_COMMAND";
		public static const ZOOM:String = "ZOOM_COMMAND";
		public static const RESIZE:String = "RESIZE_COMMAND";
		public static const RESET_ZOOM:String = "RESTORE_ZOOM";
		public static const MOVE:String = "MOVE_COMMAND";
		public static const SHARE_PRESENTATION_COMMAND:String = "SHARE_PRESENTATION_COMMAND";
		public static const SEND_CURSOR_UPDATE:String = "SEND_CURSOR_UPDATE";
		
		//Parameter for the slide navigation events
		public var slideNumber:Number;
		
		//Parameters for the zoom event
		public var zoomPercentage:Number;
		
		//Parameters for the resize event
		public var newSizeInPercent:Number;
		
		//Parameters for the cursor event
		public var xPercent:Number;
		public var yPercent:Number;
		
		//Parameters for the move event
		public var xOffset:Number;
		public var yOffset:Number;
		
		public var slideToCanvasWidthRatio:Number;
		public var slideToCanvasHeightRatio:Number;
		
		//Parameters for the share event
		public var presentationName:String;
		public var share:Boolean;
		
		public function PresenterCommands(type:String, slideNumber:Number = 0)
		{
			this.slideNumber = slideNumber;
			super(type, true, false);
		}

	}
}