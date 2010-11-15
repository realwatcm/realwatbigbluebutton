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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author leif
 */
public class XMLResponseConferenceListParser extends DefaultHandler {
    private static Logger log = Red5LoggerFactory.getLogger(XMLResponseConferenceListParser.class, "bigbluebutton");

    private List<ConferenceMember> myConfrenceMembers;
    private String tempVal;
    private ConferenceMember tempMember;
    private ConferenceMemberFlags tempFlags;
    private String room;
    private boolean inFlags = false;
    
    public XMLResponseConferenceListParser() {
        myConfrenceMembers = new ArrayList<ConferenceMember>();
    }

    public String getConferenceRoom() {
        return room;
    }

    public void printConferneceMemebers() {
        log.info("Number of Members found in room [{}] was ({}).", room, myConfrenceMembers.size());
        Iterator<ConferenceMember> it = myConfrenceMembers.iterator();
        while(it.hasNext()) {
            log.info("room [{}] member: [{}]", room, it.next().toString());
        }
    }

    public List<ConferenceMember> getConferenceList() {
        return myConfrenceMembers;
    }

            /*
<?xml version="1.0"?>
<conferences>
  <conference name="3001-192.168.1.10" member-count="1" rate="8000" running="true" answered="true" enforce_min="true" dynamic="true">
    <members>
      <member>
        <id>6</id>
        <flags>
          <can_hear>true</can_hear>
          <can_speak>true</can_speak>
          <talking>false</talking>
          <has_video>false</has_video>
          <has_floor>true</has_floor>
          <is_moderator>false</is_moderator>
          <end_conference>false</end_conference>
        </flags>
        <uuid>3a16f061-0df6-45d5-b401-d8e977e08a5c</uuid>
        <caller_id_name>1001</caller_id_name>
        <caller_id_number>1001</caller_id_number>
        <join_time>65</join_time>
        <last_talking>4</last_talking>
      </member>
    </members>
  </conference>
</conferences>

             */


    //SAX Event Handlers
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //reset
        inFlags = false;
        tempVal = "";
        if(qName.equalsIgnoreCase("member")) {
            //create a new instance of ConferenceMember
            tempMember = new ConferenceMember();
        }

        if(qName.equalsIgnoreCase("flags")) {
            //create a new instance of ConferenceMember
            tempFlags = new ConferenceMemberFlags();
            inFlags = true;
        }

        if(qName.equalsIgnoreCase("conference")) {
            room = attributes.getValue("name");
        }
    }


    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch,start,length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

        if(qName.equalsIgnoreCase("member")) {
            //add it to the list
            myConfrenceMembers.add(tempMember);
        }else if(qName.equalsIgnoreCase("flags")) {
            tempMember.setFlags(tempFlags);
            inFlags = false;
        }else if(inFlags) {
            if (qName.equalsIgnoreCase("can_speak")) {
                tempFlags.setCanSpeak(tempVal);
            }else if (qName.equalsIgnoreCase("talking")) {
                tempFlags.setTalking(tempVal);
            }
        }else if (qName.equalsIgnoreCase("id")) {
            try {
                tempMember.setId(Integer.parseInt(tempVal));
            } catch(NumberFormatException nfe) {
                log.error("cannot set ConferenceMember Id value [{}] NFE.", tempVal);
            }
        }else if (qName.equalsIgnoreCase("uuid")) {
            tempMember.setUUID(tempVal);
        }else if (qName.equalsIgnoreCase("caller_id_name")) {
            tempMember.setCallerIdName(tempVal);
        }else if (qName.equalsIgnoreCase("caller_id_number")) {
            tempMember.setCallerId(tempVal);
        }else if (qName.equalsIgnoreCase("join_time")) {
            try {
                tempMember.setJoinTime(Integer.parseInt(tempVal));
            } catch(NumberFormatException nfe) {
                log.debug("cannot set setJoinTime value [{}] NFE.", tempVal);
            }
        }else if (qName.equalsIgnoreCase("last_talking")) {
            try {
                tempMember.setLastTalking(Integer.parseInt(tempVal));
            } catch(NumberFormatException nfe) {
                log.debug("cannot set setLastTalking value [{}] NFE.", tempVal);
            }
        }

    }
}
