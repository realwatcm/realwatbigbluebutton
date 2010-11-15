/*
 * BigBlueButton - http://www.bigbluebutton.org
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
 * $Id: $
 */
package org.bigbluebutton.modules.chat.events
{
	import flash.events.Event;
	import flash.utils.Dictionary;

	public class ChatEvent extends Event
	{
		public static const CHAT_EVENT:String = 'CHAT_EVENT';
		public static const NEW_CHAT_MESSAGE_EVENT:String = 'NEW_CHAT_MESSAGE_EVENT';
		public static const PRIVATE_CHAT_MESSAGE_EVENT:String = 'PRIVATE_CHAT_MESSAGE_EVENT';
		
		public function ChatEvent(type:String, bubbles:Boolean=true, cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
		}
		
	}
}