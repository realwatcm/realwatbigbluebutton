/* BigBlueButton - http://www.bigbluebutton.org
 * 
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
 * @author Jeremy Thomerson <jthomerson@genericconf.com>
 * @version $Id: $
 */
package org.bigbluebutton.web.controllers


import java.text.MessageFormat;
import java.util.Collections;
import org.apache.commons.codec.binary.Hex;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import org.bigbluebutton.web.services.DynamicConferenceService;
import org.bigbluebutton.api.domain.DynamicConference;
import org.bigbluebutton.conference.Room
import org.bigbluebutton.api.IApiConferenceEventListener;

import org.codehaus.groovy.grails.commons.ConfigurationHolder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ApiController {
    private static final Integer SESSION_TIMEOUT = 10800  // 3 hours
    
	private static final String CONTROLLER_NAME = 'ApiController'		
	private static final String RESP_CODE_SUCCESS = 'SUCCESS'
	private static final String RESP_CODE_FAILED = 'FAILED'
	private static final String ROLE_MODERATOR = "MODERATOR";
	private static final String ROLE_ATTENDEE = "VIEWER";

	private static final String SECURITY_SALT = '639259d4-9dd8-4b25-bf01-95f9567eaf4b'

	def DIAL_NUM = /%%DIALNUM%%/
	def CONF_NUM = /%%CONFNUM%%/
	def CONF_NAME = /%%CONFNAME%%/
	
	def keywordList = [DIAL_NUM, CONF_NUM, CONF_NAME];
		
	DynamicConferenceService dynamicConferenceService;
	IApiConferenceEventListener conferenceEventListener;

	/* general methods */
	def index = {
			log.debug CONTROLLER_NAME + "#index"
			response.addHeader("Cache-Control", "no-cache")
			withFormat {	
				xml {
					render(contentType:"text/xml") {
						response() {
							returncode(RESP_CODE_SUCCESS)
							version(dynamicConferenceService.apiVersion)
						}
					}
				}
			}
	}

	/* interface (API) methods */
	def create = {
		log.debug CONTROLLER_NAME + "#create"

		if (!doChecksumSecurity("create")) {
			invalidChecksum(); return;
		}

		String name = params.name
		if (name == null) {
			invalid("missingParamName", "You must specify a name for the meeting.");
			return
		}
		
		String mtgID = params.meetingID
		if (StringUtils.isEmpty(mtgID)) {
			invalid("missingParamMeetingID", "You must specify a meeting ID for the meeting.");
			return
		}
		log.debug("passed parameter validation - creating conference");
		String attPW = params.attendeePW
		String modPW = params.moderatorPW
		String voiceBr = params.voiceBridge
		String welcomeMessage = params.welcome
		String dialNumber = params.dialNumber
		String logoutUrl = params.logoutURL
		
		Integer maxParts = -1;
		try {
			maxParts = Integer.parseInt(params.maxParticipants);
		} catch(Exception ex) { /* do nothing */ }
		String mmRoom = params.meetmeRoom
		String mmServer = params.meetmeServer

		// check for existing:
		DynamicConference existing = dynamicConferenceService.getConferenceByMeetingID(mtgID);
		if (existing != null) {
			log.debug "Existing conference found"
			if (existing.getAttendeePassword().equals(attPW) && existing.getModeratorPassword().equals(modPW)) {
				// trying to create a conference a second time
				// return success, but give extra info
				respondWithConference(existing, "duplicateWarning", "This conference was already in existence and may currently be in progress.");
			} else {
				// enforce meetingID unique-ness
				invalid("idNotUnique", "A meeting already exists with that meeting ID.  Please use a different meeting ID.");
			}
			return;
		}
		if (StringUtils.isEmpty(attPW)) {
			attPW = RandomStringUtils.randomAlphanumeric(8);
		}
		if (StringUtils.isEmpty(modPW)) {
			modPW = RandomStringUtils.randomAlphanumeric(8);
		}
		DynamicConference conf = new DynamicConference(name, mtgID, attPW, modPW, maxParts)
		conf.setVoiceBridge(voiceBr == null || voiceBr == "" ? mtgID : voiceBr)
		
		if ((dynamicConferenceService.testVoiceBridge != null) && (conf.voiceBridge == dynamicConferenceService.testVoiceBridge)) {
			if (dynamicConferenceService.testConferenceMock != null) 
				conf.meetingToken = dynamicConferenceService.testConferenceMock
			else
				log.warn("Cannot set test conference because it is not set in bigbluebutton.properties")	
		} 
		
		if ((logoutUrl != null) || (logoutUrl != "")) {
			conf.logoutUrl = logoutUrl
		}
		
		if (welcomeMessage == null || welcomeMessage == "") {
			welcomeMessage = dynamicConferenceService.defaultWelcomeMessage
		}

		if ((dialNumber == null) || (dialNumber == "")) {
			dialNumber = dynamicConferenceService.defaultDialAccessNumber
		}

		if (welcomeMessage != null || welcomeMessage != "") {
			log.debug "Substituting keywords"
			
			keywordList.each{ keyword ->
				switch(keyword){
					case DIAL_NUM:
						if ((dialNumber != null) || (dialNumber != "")) {
							welcomeMessage = welcomeMessage.replaceAll(DIAL_NUM, dialNumber)
						}
						break
					case CONF_NUM:
						welcomeMessage = welcomeMessage.replaceAll(CONF_NUM, conf.voiceBridge)
						break
					case CONF_NAME:
						welcomeMessage = welcomeMessage.replaceAll(CONF_NAME, conf.name)
						break
				}			  
			}
		}
				
		conf.welcome = welcomeMessage
								
		log.debug("Conference created: " + conf);
		// TODO: support voiceBridge and voiceServer

		// success!
		dynamicConferenceService.storeConference(conf);
		respondWithConference(conf, null, null)
	}

	def join = {
			
		println "Entered Join"
		log.debug CONTROLLER_NAME + "#join"

		if (!doChecksumSecurity("join")) {
			invalidChecksum(); return;
		}

		String fullName = params.fullName
		if (fullName == null) {
			invalid("missingParamFullName", "You must specify a name for the attendee who will be joining the meeting.");
			return
		}
		
		String webVoice = params.webVoiceConf
		String mtgID = params.meetingID
		String attPW = params.password
		boolean redirectImm = parseBoolean(params.redirectImmediately)
		String externUserID = params.userID
		if ((externUserID == null) || (externUserID == "")) {
			externUserID = RandomStringUtils.randomAlphanumeric(12).toLowerCase()
		}
        
		// check for existing:
		DynamicConference conf = dynamicConferenceService.getConferenceByMeetingID(mtgID);
		if (conf == null) {
			invalid("invalidMeetingIdentifier", "The meeting ID that you supplied did not match any existing meetings");
			return;
		}
		
		if (conf.isForciblyEnded()) {
			invalid("meetingForciblyEnded", "You can not re-join a meeting that has already been forcibly ended.  However, once the meeting is removed from memory (according to the timeout configured on this server, you will be able to once again create a meeting with the same meeting ID");
			return;
		}

		String role = null;
		if (conf.getModeratorPassword().equals(attPW)) {
			role = ROLE_MODERATOR;
		} else if (conf.getAttendeePassword().equals(attPW)) {
			role = ROLE_ATTENDEE;
		}
		if (role == null) {
			invalidPassword("You either did not supply a password or the password supplied is neither the attendee or moderator password for this conference."); return;
		}
		
		conf.setWebVoiceConf(webVoice == null || webVoice == "" ? conf.voiceBridge : webVoice)
		
		// TODO: success....
		log.debug "join successful - setting session parameters and redirecting to join"
		session["conferencename"] = conf.meetingID
		session["meetingID"] = conf.meetingID
		session["externUserID"] = externUserID
		session["fullname"] = fullName 
		session["role"] = role
		session["conference"] = conf.getMeetingToken()
		session["room"] = conf.getMeetingToken()
		session["voicebridge"] = conf.getVoiceBridge()
		session["webvoiceconf"] = conf.getWebVoiceConf()
		session["mode"] = "LIVE"
		session["record"] = false
		session['welcome'] = conf.welcome
		
		session.setMaxInactiveInterval(SESSION_TIMEOUT);
		
    	def config = ConfigurationHolder.config
    	def hostURL = config.bigbluebutton.web.serverURL
    	redirect(url:"${hostURL}/client/BigBlueButton.html")
		
		println "Leaving Join"
	}

	def isMeetingRunning = {
		log.debug CONTROLLER_NAME + "#isMeetingRunning"

		if (!doChecksumSecurity("isMeetingRunning")) {
			invalidChecksum(); return;
		}

		String mtgID = params.meetingID

		// check for existing:
		DynamicConference conf = dynamicConferenceService.getConferenceByMeetingID(mtgID);
		boolean isRunning = conf != null && conf.isRunning();
		response.addHeader("Cache-Control", "no-cache")
		withFormat {	
			xml {
				render(contentType:"text/xml") {
					response() {
						returncode(RESP_CODE_SUCCESS)
						running(isRunning ? "true" : "false")
					}
				}
			}
		}
	}

	def end = {
		log.debug CONTROLLER_NAME + "#end"

		if (!doChecksumSecurity("end")) {
			invalidChecksum(); return;
		}

		String mtgID = params.meetingID
		String callPW = params.password

		// check for existing:
		DynamicConference conf = dynamicConferenceService.getConferenceByMeetingID(mtgID);
		Room room = dynamicConferenceService.getRoomByMeetingID(mtgID);
		
		if (conf == null || room == null) {
			invalid("notFound", "We could not find a meeting with that meeting ID - perhaps the meeting is not yet running?");
			return;
		}
		
		if (conf.getModeratorPassword().equals(callPW) == false) {
			invalidPassword("You must supply the moderator password for this call."); return;
		}
		
		conf.setForciblyEnded(true);
		
		conferenceEventListener.endMeetingRequest(room);
		
		response.addHeader("Cache-Control", "no-cache")
		withFormat {	
			xml {
				render(contentType:"text/xml") {
					response() {
						returncode(RESP_CODE_SUCCESS)
						messageKey("sentEndMeetingRequest")
						message("A request to end the meeting was sent.  Please wait a few seconds, and then use the getMeetingInfo or isMeetingRunning API calls to verify that it was ended.")
					}
				}
			}
		}
	}

	def getMeetingInfo = {
		log.debug CONTROLLER_NAME + "#getMeetingInfo"

		if (!doChecksumSecurity("getMeetingInfo")) {
			invalidChecksum(); return;
		}

		String mtgID = params.meetingID
		String callPW = params.password

		// check for existing:
		DynamicConference conf = dynamicConferenceService.getConferenceByMeetingID(mtgID);
		Room room = dynamicConferenceService.getRoomByMeetingID(mtgID);
		
		if (conf == null) {
			invalid("notFound", "We could not find a meeting with that meeting ID");
			return;
		}
		
		if (conf.getModeratorPassword().equals(callPW) == false) {
			invalidPassword("You must supply the moderator password for this call."); return;
		}

		respondWithConferenceDetails(conf, room, null, null);
	}
	
	def getMeetings = {
		log.debug CONTROLLER_NAME + "#getMeetings"

		if (!doChecksumSecurity("getMeetings")) {
			invalidChecksum(); return;
		}

		// check for existing:
		Collection<DynamicConference> confs = dynamicConferenceService.getAllConferences();
		
		if (confs == null || confs.size() == 0) {
			response.addHeader("Cache-Control", "no-cache")
			withFormat {	
				xml {
					log.debug "Rendering as xml"
					render(contentType:"text/xml") {
						response() {
							returncode(RESP_CODE_SUCCESS)
							meetings(null)
							messageKey("noMeetings")
							message("no meetings were found on this server")
						}
					}
				}
			}
			return;
		}
		
		response.addHeader("Cache-Control", "no-cache")
		withFormat {	
			xml {
				log.debug "Rendering as xml"
				render(contentType:"text/xml") {
					response() {
						returncode(RESP_CODE_SUCCESS)
						meetings() {
							confs.each { conf ->
								meeting() {
									meetingID("${conf.meetingID}")
									attendeePW("${conf.attendeePassword}")
									moderatorPW("${conf.moderatorPassword}")
									hasBeenForciblyEnded(conf.isForciblyEnded() ? "true" : "false")
									running(conf.isRunning() ? "true" : "false")
								}
							}
						}
					}
				}
			}
		}
	}

	def enter = {
		
		println "Entered Enter"
		
		def fname = session["fullname"]
	    def rl = session["role"]
	    def cnf = session["conference"]
	    def rm = session["room"]
		def vb = session["voicebridge"] 
		def wbv = session["webvoiceconf"]  
	    def rec = session["record"]
	    def md = session["mode"]
	    def confName = session["conferencename"]
	    def welcomeMsg = session['welcome']
	    def meetID = session["meetingID"] 
        def externUID = session["externUserID"] 
        
        println "After reading from cookie"
        
        
	    if (!rm) {
	    	println "Could not find conference"
	    	response.addHeader("Cache-Control", "no-cache")
	    	withFormat {				
				xml {
					render(contentType:"text/xml") {
						response() {
							returncode("FAILED")
							message("Could not find conference ${params.conference}.")
						}
					}
				}
			}
	    } else {	
	    	println "Found conference"
	    	response.addHeader("Cache-Control", "no-cache")
	    	withFormat {				
				xml {
					render(contentType:"text/xml") {
						response() {
							returncode("SUCCESS")
							fullname("$fname")
							confname("$confName")
							meetingID("$meetID")
							externUserID("$externUID")
	        				role("$rl")
	        				conference("$cnf")
	        				room("$rm")
	        				voicebridge("${vb}")
	        				webvoiceconf("${wbv}")
	        				mode("$md")
	        				record("$rec")
	        				welcome("$welcomeMsg")
						}
					}
				}
			}
	    }  
		println "Leaving Enter"
	}
	
	def signOut = {
	    def config = ConfigurationHolder.config
        def hostURL = config.bigbluebutton.web.logoutURL
        
        log.debug("LogoutURL=$hostURL")
        
        // For backward compatibility. We renamed "loggedOutUrl" to
        // "logoutURL" in 0.64 to be consistent with the API. Remove this
        // in later iterations (ralam mar 26, 2010)
        //if ((hostURL == null) || (hostURL == "")) {
        if (hostURL.isEmpty()) {
            log.debug("No logoutURL property set. Checking for old loggedOutUrl.")
            hostURL = config.bigbluebutton.web.loggedOutUrl
            if (!hostURL.isEmpty()) 
               log.debug("Old loggedOutUrl property set to $hostURL") 
        }
        
	    def meetingToken = session["conference"]
        DynamicConference conf = dynamicConferenceService.getConferenceByToken(meetingToken)
        if (conf != null) {
        	if ((conf.logoutUrl != null) && (conf.logoutUrl != "")) {
        	   hostURL = conf.logoutUrl
        	   log.debug("logoutURL has been set from API. Redirecting to server url $hostURL.")
    		}
        }
	    	    
        if (hostURL.isEmpty()) {           
        	hostURL = config.bigbluebutton.web.serverURL
        	log.debug("No logout url. Redirecting to server url $hostURL.")
        }
        // Log the user out of the application.
	    session.invalidate()
        println "serverURL $hostURL"	
	    redirect(url: hostURL)
	}
	
	/* helper methods */
	def doChecksumSecurity(callName) {
		log.debug CONTROLLER_NAME + "#doChecksumSecurity"
		log.debug "checksum: " + params.checksum + "; query string: " + request.getQueryString()
		if (StringUtils.isEmpty(callName)) {
			throw new RuntimeException("Programming error - you must pass the call name to doChecksumSecurity so it can be used in the checksum");
		}
		if (StringUtils.isEmpty(request.getQueryString())) {
			invalid("noQueryString", "No query string was found in your request.")
			return false;
		}
	
		if (StringUtils.isEmpty(securitySalt()) == false) {
			String qs = request.getQueryString()
			// handle either checksum as first or middle / end parameter
			// TODO: this is hackish - should be done better
			qs = qs.replace("&checksum=" + params.checksum, "")
			qs = qs.replace("checksum=" + params.checksum + "&", "")
			log.debug "query string after checksum removed: " + qs
			String cs = DigestUtils.shaHex(callName + qs + securitySalt());
			log.debug "our checksum: " + cs
			if (cs == null || cs.equals(params.checksum) == false) {
				log.info("checksumError: request did not pass the checksum security check")
				return false;
			}
			log.debug("checksum ok: request passed the checksum security check")
			return true; 
		}
		
		log.warn "Security is disabled in this service. Make sure this is intentional."
		return true;
	}

	private String securitySalt() {
		return dynamicConferenceService.securitySalt
	}
	
	def beforeInterceptor = {
		if (dynamicConferenceService.serviceEnabled == false) {
			log.info("apiNotEnabled: The API service and/or controller is not enabled on this server.  To use it, you must first enable it.")
			// TODO: this doesn't stop the request - so it generates invalid XML
			//			since the request continues and renders a second response
			invalid("apiNotEnabled", "The API service and/or controller is not enabled on this server.  To use it, you must first enable it.")
		}
	}

	def respondWithConferenceDetails(conf, room, msgKey, msg) {
		response.addHeader("Cache-Control", "no-cache")
		withFormat {				
			xml {
				render(contentType:"text/xml") {
					response() {
						returncode(RESP_CODE_SUCCESS)
						meetingID("${conf.meetingID}")
						attendeePW("${conf.attendeePassword}")
						moderatorPW("${conf.moderatorPassword}")
						running(conf.isRunning() ? "true" : "false")
						hasBeenForciblyEnded(conf.isForciblyEnded() ? "true" : "false")
						startTime("${conf.startTime}")
						endTime("${conf.endTime}")
						participantCount(room == null ? 0 : room.getNumberOfParticipants())
						moderatorCount(room == null ? 0 : room.getNumberOfModerators())
						attendees() {
							room == null ? Collections.emptyList() : room.participantCollection.each { att ->
								attendee() {
									userID("${att.externUserID}")
									fullName("${att.name}")
									role("${att.role}")
								}
							}
						}
						messageKey(msgKey == null ? "" : msgKey)
						message(msg == null ? "" : msg)
					}
				}
			}
		}			 
	}

	def respondWithConference(conf, msgKey, msg) {
		response.addHeader("Cache-Control", "no-cache")
		withFormat {	
			xml {
				log.debug "Rendering as xml"
				render(contentType:"text/xml") {
					response() {
						returncode(RESP_CODE_SUCCESS)
						meetingID("${conf.meetingID}")
						attendeePW("${conf.attendeePassword}")
						moderatorPW("${conf.moderatorPassword}")
						hasBeenForciblyEnded(conf.isForciblyEnded() ? "true" : "false")
						messageKey(msgKey == null ? "" : msgKey)
						message(msg == null ? "" : msg)
					}
				}
			}
		}
	}
	
	def invalidPassword(msg) {
		invalid("invalidPassword", msg);
	}
	
	def invalidChecksum() {
		invalid("checksumError", "You did not pass the checksum security check")
	}
	
	def invalid(key, msg) {
		log.debug CONTROLLER_NAME + "#invalid"
		response.addHeader("Cache-Control", "no-cache")
		withFormat {				
			xml {
				render(contentType:"text/xml") {
					response() {
						returncode(RESP_CODE_FAILED)
						messageKey(key)
						message(msg)
					}
				}
			}
			json {
				log.debug "Rendering as json"
				render(contentType:"text/json") {
						returncode(RESP_CODE_FAILED)
						messageKey(key)
						message(msg)
				}
			}
		}			 
	}

	def parseBoolean(obj) {
		if (obj instanceof Number) {
			return ((Number) obj).intValue() == 1;
		}
		return false
	}
}
