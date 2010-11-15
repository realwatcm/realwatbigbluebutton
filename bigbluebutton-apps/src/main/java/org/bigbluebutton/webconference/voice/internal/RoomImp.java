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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bigbluebutton.webconference.voice.Participant;
import org.bigbluebutton.webconference.voice.Room;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class RoomImp implements Room {
	private final String name;
	private final ConcurrentMap<Integer, Participant> participants;
	
	private boolean muted = false;
	
	public RoomImp(String name) {
		this.name = name;
		participants = new ConcurrentHashMap<Integer, Participant>();
	}
	
	public String getName() {
		return name;
	}
	
	public int countParticipants() {
		return participants.size();
	}
	
	public Participant getParticipant(Integer id) {
		return participants.get(id);
	}
	
	public Participant add(Participant p) {
		return participants.putIfAbsent(p.getId(), p);
	}
	
	public boolean hasParticipant(Integer id) {
		return participants.containsKey(id);
	}
	
	public void remove(Integer id) {
		Participant p = participants.remove(id);
		if (p != null) p = null;
	}
	
	public void mute(boolean mute) {
		muted = mute;
	}
	
	public boolean isMuted() {
		return muted;
	}
	
	public ArrayList<Participant> getParticipants() {
		Map<Integer, Participant> p = Collections.unmodifiableMap(participants);
		ArrayList<Participant> pa = new ArrayList<Participant>();
		pa.addAll(p.values());
		return pa;
	}
}
