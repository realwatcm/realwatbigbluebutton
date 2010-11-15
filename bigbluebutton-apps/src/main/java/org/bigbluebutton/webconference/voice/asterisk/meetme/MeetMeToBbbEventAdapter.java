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
package org.bigbluebutton.webconference.voice.asterisk.meetme;

import org.asteriskjava.manager.event.AbstractMeetMeEvent;
import org.asteriskjava.manager.event.MeetMeJoinEvent;
import org.asteriskjava.manager.event.MeetMeLeaveEvent;
import org.asteriskjava.manager.event.MeetMeMuteEvent;
import org.asteriskjava.manager.event.MeetMeTalkingEvent;
import org.bigbluebutton.webconference.voice.events.ConferenceEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantJoinedEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantLeftEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantMutedEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantTalkingEvent;
import org.bigbluebutton.webconference.voice.events.UnknownConferenceEvent;

/**
 * This class transforms AppKonference events into BigBlueButton Voice
 * Conference Events.
 * 
 * @author Richard Alam
 *
 */
public class MeetMeToBbbEventAdapter {

	/*
	 * Transforms MeetMeEvents into BBB Voice Conference Events.
	 * Return UnknownConferenceEvent if unable to transform the event.
	 */
	public ConferenceEvent transform(AbstractMeetMeEvent event) {		
		if (event instanceof MeetMeJoinEvent) {
			MeetMeJoinEvent mmj = (MeetMeJoinEvent) event;

			ParticipantJoinedEvent pj = new ParticipantJoinedEvent(mmj.getUserNum(), mmj.getMeetMe(),
					mmj.getCallerIdNum(), mmj.getCallerIdName(), false, false);
			return pj;
		} else if (event instanceof MeetMeLeaveEvent) {
			MeetMeLeaveEvent mml = (MeetMeLeaveEvent) event;
			ParticipantLeftEvent pl = new ParticipantLeftEvent(mml.getUserNum(), mml.getMeetMe());
			return pl;
		} else if (event instanceof MeetMeMuteEvent) {
			MeetMeMuteEvent mmm = (MeetMeMuteEvent) event;
			ParticipantMutedEvent pm = new ParticipantMutedEvent(mmm.getUserNum(), mmm.getMeetMe(), mmm.getStatus());
			return pm;
		} else if (event instanceof MeetMeTalkingEvent) {
			MeetMeTalkingEvent mmt = (MeetMeTalkingEvent) event;
			ParticipantTalkingEvent pt = new ParticipantTalkingEvent(mmt.getUserNum(), mmt.getMeetMe(), mmt.getStatus());
			return pt;
		}	
		return new UnknownConferenceEvent(new Integer(0),"unknown");
	}
}
