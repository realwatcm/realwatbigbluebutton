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

package org.bigbluebutton.webconference.voice.freeswitch.response;

/**
 *
 * @author leif
 */
public class ConferenceMember {

    protected Integer memberId;
    protected ConferenceMemberFlags flags;
    protected String uuid;
    protected String callerIdName;
    protected String callerId;
    protected Integer joinTime;
    protected Integer lastTalking;

    public Integer getId() {
        return memberId;
    }

    public ConferenceMemberFlags getFlags() {
        return flags;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getCallerIdName() {
        return callerIdName;
    }

    public boolean getMuted() {
        return flags.getIsMuted();
    }

    public boolean getSpeeking() {
        return flags.getIsSpeeking();
    }

    public void setFlags(ConferenceMemberFlags flags) {
        this.flags = flags;
    }

    public void setId(int parseInt) {
        memberId = parseInt;
    }

    public void setUUID(String tempVal) {
        this.uuid = tempVal;
    }

    public void setCallerIdName(String tempVal) {
        this.callerIdName = tempVal;
    }

    public void setCallerId(String tempVal) {
        this.callerId = tempVal;
    }

    public void setJoinTime(int parseInt) {
        this.joinTime = parseInt;
    }

    void setLastTalking(int parseInt) {
        this.lastTalking = parseInt;
    }

}
