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

package org.bigbluebutton.conference.service.presentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.red5.logging.Red5LoggerFactory;

import net.jcip.annotations.ThreadSafe;import java.util.concurrent.ConcurrentHashMap;import java.util.concurrent.CopyOnWriteArrayList;import java.util.ArrayList;
import java.util.Collections;import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * Contains information about a PresentationRoom. 
 */
@ThreadSafe
public class PresentationRoom {
	private static Logger log = Red5LoggerFactory.getLogger( PresentationRoom.class, "bigbluebutton" );
	
	private final String name;
	private final Map<String, IPresentationRoomListener> listeners;
	
	//TODO: check this type of attributes...
	@SuppressWarnings("unchecked")
	ArrayList currentPresenter = null;
	int currentSlide = 0;
	Boolean sharing = false;
	String currentPresentation = "";
	Double xOffset = 0D;
	Double yOffset = 0D;
	Double widthRatio = 0D;
	Double heightRatio = 0D;
	ArrayList<String> presentationNames = new ArrayList<String>();
	
	public PresentationRoom(String name) {
		this.name = name;
		listeners   = new ConcurrentHashMap<String, IPresentationRoomListener>();
	}
	
	public String getName() {
		return name;
	}
	
	public void addRoomListener(IPresentationRoomListener listener) {
		if (! listeners.containsKey(listener.getName())) {
			log.debug("adding room listener");
			listeners.put(listener.getName(), listener);			
		}
	}
	
	public void removeRoomListener(IPresentationRoomListener listener) {
		log.debug("removing room listener");
		listeners.remove(listener);		
	}
	
	@SuppressWarnings("unchecked")
	public void sendUpdateMessage(Map message){
		for (Iterator iter = listeners.values().iterator(); iter.hasNext();) {
			log.debug("calling on listener");
			IPresentationRoomListener listener = (IPresentationRoomListener) iter.next();
			log.debug("calling sendUpdateMessage on listener {}",listener.getName());
			listener.sendUpdateMessage(message);
		}	
		
		storePresentationNames(message);
	}

	@SuppressWarnings("unchecked")
	private void storePresentationNames(Map message){
        String presentationName = (String) message.get("presentationName");
        String messageKey = (String) message.get("messageKey");
             
        if (messageKey.equalsIgnoreCase("CONVERSION_COMPLETED")) {            
            log.debug("{}[{}]",messageKey,presentationName);
            presentationNames.add(presentationName);                                
        }           
    }
	
	public void resizeAndMoveSlide(Double xOffset, Double yOffset, Double widthRatio, Double heightRatio) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.widthRatio = widthRatio;
		this.heightRatio = heightRatio;
		
		for (Iterator iter = listeners.values().iterator(); iter.hasNext();) {
			log.debug("calling on listener");
			IPresentationRoomListener listener = (IPresentationRoomListener) iter.next();
			log.debug("calling sendUpdateMessage on listener {}",listener.getName());
			listener.resizeAndMoveSlide(xOffset, yOffset, widthRatio, heightRatio);
		}		
	}
	
	public void assignPresenter(ArrayList presenter){
		currentPresenter = presenter;
		for (Iterator iter = listeners.values().iterator(); iter.hasNext();) {
			log.debug("calling on listener");
			IPresentationRoomListener listener = (IPresentationRoomListener) iter.next();
			log.debug("calling sendUpdateMessage on listener {}",listener.getName());
			listener.assignPresenter(presenter);
		}	
	}
	
	@SuppressWarnings("unchecked")
	public void gotoSlide(int curslide){
		log.debug("Request to go to slide $it for room $name");
		currentSlide = curslide;
		for (Iterator iter = listeners.values().iterator(); iter.hasNext();) {
			log.debug("calling on listener");
			IPresentationRoomListener listener = (IPresentationRoomListener) iter.next();
			log.debug("calling sendUpdateMessage on listener {}",listener.getName());
			listener.gotoSlide(curslide);
		}			
	}	
	
	@SuppressWarnings("unchecked")
	public void sharePresentation(String presentationName, Boolean share){
		log.debug("Request share presentation "+presentationName+" "+share+" for room "+name);
		sharing = share;
		if (share) {
		  currentPresentation = presentationName;
		} else {
		  currentPresentation = "";
		}
		 
		for (Iterator iter = listeners.values().iterator(); iter.hasNext();) {
			log.debug("calling on listener");
			IPresentationRoomListener listener = (IPresentationRoomListener) iter.next();
			log.debug("calling sharePresentation on listener {}",listener.getName());
			listener.sharePresentation(presentationName, share);
		}			
	}
	    
    public void removePresentation(String presentationName){
        log.debug("Request remove presentation {}",presentationName);
        int index = presentationNames.indexOf(presentationName);
        
        if (index < 0) {
            log.warn("Request remove presentation {}. Presentation not found.",presentationName);
            return;
        }
        
        presentationNames.remove(index);
        
        for (Iterator iter = listeners.values().iterator(); iter.hasNext();) {
            log.debug("calling on listener");
            IPresentationRoomListener listener = (IPresentationRoomListener) iter.next();
            log.debug("calling removePresentation on listener {}",listener.getName());
            listener.removePresentation(presentationName);
        }   
        
        if (currentPresentation == presentationName) {
            sharePresentation(presentationName, false);
        }        
    }
    
    public String getCurrentPresentation() {
		return currentPresentation;
	}

	public int getCurrentSlide() {
		return currentSlide;
	}

	public Boolean getSharing() {
		return sharing;
	}

	public ArrayList<String> getPresentationNames() {
		return presentationNames;
	}

	public ArrayList getCurrentPresenter() {
		return currentPresenter;
	}

	public Double getxOffset() {
		return xOffset;
	}

	public Double getyOffset() {
		return yOffset;
	}

	public Double getWidthRatio() {
		return widthRatio;
	}

	public Double getHeightRatio() {
		return heightRatio;
	}
}
