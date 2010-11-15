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
package org.bigbluebutton.webconference.voice.internal;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.bigbluebutton.webconference.voice.ConferenceService;
import org.bigbluebutton.webconference.voice.Participant;
import org.bigbluebutton.webconference.voice.events.ConferenceEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantJoinedEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantLeftEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantLockedEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantMutedEvent;
import org.bigbluebutton.webconference.voice.events.ParticipantTalkingEvent;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class RoomManager {
	private static final Logger log = Red5LoggerFactory.getLogger(RoomManager.class, "bigbluebutton");
	
	private final ConcurrentHashMap<String, RoomImp> rooms;
	private final ConferenceService confService;
	
	public RoomManager(ConferenceService service) {
		rooms = new ConcurrentHashMap<String, RoomImp>();
		confService = service;
	}
	
	public void createRoom(String name) {
		log.debug("Creating room: " + name);
		RoomImp r = new RoomImp(name);
		rooms.putIfAbsent(name, r);
	}
	
	public boolean hasRoom(String name) {
		return rooms.containsKey(name);
	}
	
	public boolean hasParticipant(String room, Integer participant) {
		RoomImp rm = rooms.get(room);
		if (rm == null) return false;
		return rm.hasParticipant(participant);
	}
	
	public void destroyRoom(String name) {
		log.debug("Destorying room: + name");
		RoomImp r = rooms.remove(name);
		if (r != null) r = null;
	}
	
	public void mute(String room, boolean mute) {
		RoomImp rm = rooms.get(room);
		if (rm != null) rm.mute(mute);
	}
	
	public boolean isRoomMuted(String room){
		RoomImp rm = rooms.get(room);
		if (rm != null) return rm.isMuted();
		else return false;
	}
	
	public ArrayList<Participant> getParticipants(String room) {
		log.debug("Getting participants for room: " + room);
		RoomImp rm = rooms.get(room);		
		if (rm == null) {
			log.info("Getting participants for non-existing room: " + room);
		}
		return rm.getParticipants();
	}
		
	public boolean isParticipantMuteLocked(Integer participant, String room) {
		RoomImp rm = rooms.get(room);
		if (rm != null) {
			Participant p = rm.getParticipant(participant);
			return p.isMuteLocked();
		}
		return false;
	}

	private void lockParticipant(String room, Integer participant, Boolean lock) {
		RoomImp rm = rooms.get(room);
		if (rm != null) {
			ParticipantImp p = (ParticipantImp) rm.getParticipant(participant);
			if (p != null)
				p.setLock(lock);
		}
	}
	
	public void processConferenceEvent(ConferenceEvent event) {
		log.debug("Processing event for room: " + event.getRoom());
		RoomImp rm = rooms.get(event.getRoom());
		if (rm == null) {
			log.info("Processing event for non-existing room: " + event.getRoom());
			return;
		}
		
		if (event instanceof ParticipantJoinedEvent) {
			log.debug("Processing ParticipantJoinedEvent for room: " + event.getRoom());
			ParticipantJoinedEvent pje = (ParticipantJoinedEvent) event;
			ParticipantImp p = new ParticipantImp(pje.getParticipantId(), pje.getCallerIdName());
			p.setMuted(pje.getMuted());
			p.setTalking(pje.getSpeaking());
			log.debug("Joined [" + p.getId() + "," + p.getName() + "," + p.isMuted() + "," + p.isTalking() + "] to room " + rm.getName());
			rm.add(p);
			if (rm.isMuted() && !p.isMuted()) {
				confService.mute(p.getId(), event.getRoom(), true);
			}
		} else if (event instanceof ParticipantLeftEvent) {		
			log.debug("Processing ParticipantLeftEvent for room: " + event.getRoom());
			rm.remove(event.getParticipantId());		
		} else if (event instanceof ParticipantMutedEvent) {
			log.debug("Processing ParticipantMutedEvent for room: " + event.getRoom());
			ParticipantMutedEvent pme = (ParticipantMutedEvent) event;
			ParticipantImp p = (ParticipantImp) rm.getParticipant(event.getParticipantId());
			if (p != null) p.setMuted(pme.isMuted());
		} else if (event instanceof ParticipantTalkingEvent) {
			log.debug("Processing ParticipantTalkingEvent for room: " + event.getRoom());
			ParticipantTalkingEvent pte = (ParticipantTalkingEvent) event;
			ParticipantImp p = (ParticipantImp) rm.getParticipant(event.getParticipantId());
			if (p != null) p.setTalking(pte.isTalking());
		} else if (event instanceof ParticipantLockedEvent) {
			ParticipantLockedEvent ple = (ParticipantLockedEvent) event;
			lockParticipant(ple.getRoom(), ple.getParticipantId(), ple.isLocked());
		} else {
			log.debug("Processing UnknowEvent " + event.getClass().getName() + " for room: " + event.getRoom() );
		}
	}
}
