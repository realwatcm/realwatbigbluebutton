/** 
*
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
*
* Copyright (c) 2010 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2.1 of the License, or (at your option) any later
* version.
*
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
* 
**/
package org.bigbluebutton.voiceconf.sip;

import org.zoolu.sip.call.*;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.message.*;
import org.zoolu.sdp.*;
import org.bigbluebutton.voiceconf.red5.CallStreamFactory;
import org.bigbluebutton.voiceconf.red5.ClientConnectionManager;
import org.bigbluebutton.voiceconf.red5.media.CallStream;
import org.bigbluebutton.voiceconf.red5.media.CallStreamObserver;
import org.bigbluebutton.voiceconf.red5.media.StreamException;
import org.bigbluebutton.voiceconf.util.StackTraceUtil;
import org.red5.app.sip.codecs.Codec;
import org.red5.app.sip.codecs.CodecUtils;
import org.slf4j.Logger;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Vector;

public class CallAgent extends CallListenerAdapter implements CallStreamObserver  {
    private static Logger log = Red5LoggerFactory.getLogger(CallAgent.class, "sip");
    
    private final SipPeerProfile userProfile;
    private final SipProvider sipProvider;
    private ExtendedCall call;
    private CallStream callStream;    
    private String localSession = null;
    private Codec sipCodec = null;    
    private CallStreamFactory callStreamFactory;
    private ClientConnectionManager clientConnManager; 
    private final String clientId;
    private final AudioConferenceProvider portProvider;
    private DatagramSocket localSocket;
    
    private enum CallState {
    	UA_IDLE(0), UA_INCOMING_CALL(1), UA_OUTGOING_CALL(2), UA_ONCALL(3);    	
    	private final int state;
    	CallState(int state) {this.state = state;}
    	private int getState() {return state;}
    }

    private CallState callState;

    public CallAgent(SipProvider sipProvider, SipPeerProfile userProfile, AudioConferenceProvider portProvider, String clientId) {
        this.sipProvider = sipProvider;
        this.userProfile = userProfile;
        this.portProvider = portProvider;
        this.clientId = clientId;
    }
    
    public String getCallId() {
    	return clientId;
    }
    
    private void initSessionDescriptor() {        
        log.debug("initSessionDescriptor");        
        SessionDescriptor newSdp = SdpUtils.createInitialSdp(userProfile.username, 
        		sipProvider.getViaAddress(), userProfile.audioPort, 
        		userProfile.videoPort, userProfile.audioCodecsPrecedence );        
        localSession = newSdp.toString();        
        log.debug("localSession Descriptor = " + localSession );
    }

    public void call(String callerName, String destination) {    	
    	log.debug("call {}", destination);  
    	try {
			localSocket = getLocalAudioSocket();
			userProfile.audioPort = localSocket.getLocalPort();	    	
		} catch (Exception e) {
			notifyListenersOnOutgoingCallFailed();
			return;
		}    	
    	
		setupCallerDisplayName(callerName, destination);	
    	userProfile.initContactAddress(sipProvider);        
        initSessionDescriptor();
        
    	callState = CallState.UA_OUTGOING_CALL;
    	
        call = new ExtendedCall(sipProvider, userProfile.fromUrl, 
                userProfile.contactUrl, userProfile.username,
                userProfile.realm, userProfile.passwd, this);  
        
        // In case of incomplete url (e.g. only 'user' is present), 
        // try to complete it.       
        destination = sipProvider.completeNameAddress(destination).toString();
        log.debug("call {}", destination);  
        if (userProfile.noOffer) {
            call.call(destination);
        } else {
            call.call(destination, localSession);
        }
    }

    private void setupCallerDisplayName(String callerName, String destination) {
    	String fromURL = "\"" + callerName + "\" <sip:" + destination + "@" + portProvider.getHost() + ">";
    	userProfile.username = callerName;
    	userProfile.fromUrl = fromURL;
		userProfile.contactUrl = "sip:" + destination + "@" + sipProvider.getViaAddress();
        if (sipProvider.getPort() != SipStack.default_port) {
            userProfile.contactUrl += ":" + sipProvider.getPort();
        }
    }
    
    /** Closes an ongoing, incoming, or pending call */
    public void hangup() {
    	log.debug("hangup");
    	
    	if (callState == CallState.UA_IDLE) return;    	
    	closeVoiceStreams();        
    	if (call != null) call.hangup();    
    	callState = CallState.UA_IDLE; 
    }

    private DatagramSocket getLocalAudioSocket() throws Exception {
    	DatagramSocket socket = null;
    	boolean failedToGetSocket = true;
    	
    	for (int i = 0; i < 3; i++) {
    		try {
        		socket = new DatagramSocket(portProvider.getFreeAudioPort());
        		failedToGetSocket = false;
        		break;
    		} catch (SocketException e) {
    			log.error("Failed to setup local audio socket.");    			
    		}
    	}
    	
    	if (failedToGetSocket) {
    		throw new Exception("Exception while initializing CallStream");
    	}
    	
    	return socket;
    }
    
    private void createVoiceStreams() {
        if (callStream != null) {            
        	log.debug("Media application is already running.");
            return;
        }
        
        SessionDescriptor localSdp = new SessionDescriptor(call.getLocalSessionDescriptor());        
        SessionDescriptor remoteSdp = new SessionDescriptor(call.getRemoteSessionDescriptor());
        String remoteMediaAddress = SessionDescriptorUtil.getRemoteMediaAddress(remoteSdp);
        int remoteAudioPort = SessionDescriptorUtil.getRemoteAudioPort(remoteSdp);
        int localAudioPort = SessionDescriptorUtil.getLocalAudioPort(localSdp);
    	
    	SipConnectInfo connInfo = new SipConnectInfo(localSocket, remoteMediaAddress, remoteAudioPort);
        
        log.debug("[localAudioPort=" + localAudioPort + ",remoteAudioPort=" + remoteAudioPort + "]");

        if (userProfile.audio && localAudioPort != 0 && remoteAudioPort != 0) {
            if ((callStream == null) && (sipCodec != null)) {               	
            	try {
					callStream = callStreamFactory.createCallStream(sipCodec, connInfo);
					callStream.addCallStreamObserver(this);
					callStream.start();
					notifyListenersOnCallConnected(callStream.getTalkStreamName(), callStream.getListenStreamName());
				} catch (Exception e) {
					log.error("Failed to create Call Stream.");
					System.out.println(StackTraceUtil.getStackTrace(e));
				}                
            }
        }
    }

        
    public void startTalkStream(IBroadcastStream broadcastStream, IScope scope) {
    	try {
			callStream.startTalkStream(broadcastStream, scope);
		} catch (StreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   	
    }
    
    public void stopTalkStream(IBroadcastStream broadcastStream, IScope scope) {
    	if (callStream != null) {
    		callStream.stopTalkStream(broadcastStream, scope);   	
    	}
    }
    
    private void closeVoiceStreams() {        
    	log.debug("closeMediaApplication" );
        
        if (callStream != null) {
        	callStream.stop();
        	callStream = null;
        }
    }

    // ********************** Call callback functions **********************
    private void createAudioCodec(SessionDescriptor newSdp) {
    	sipCodec = SdpUtils.getNegotiatedAudioCodec(newSdp);
    }
        
    private void setupSdpAndCodec(String sdp) {
    	SessionDescriptor remoteSdp = new SessionDescriptor(sdp);
        SessionDescriptor localSdp = new SessionDescriptor(localSession);
        
        log.debug("localSdp = " + localSdp.toString() + ".");
        log.debug("remoteSdp = " + remoteSdp.toString() + ".");
        
        // First we need to make payloads negotiation so the related attributes can be then matched.
        SessionDescriptor newSdp = SdpUtils.makeMediaPayloadsNegotiation(localSdp, remoteSdp);        
        createAudioCodec(newSdp);
        
        // Now we complete the SDP negotiation informing the selected 
        // codec, so it can be internally updated during the process.
        SdpUtils.completeSdpNegotiation(newSdp, localSdp, remoteSdp);
        localSession = newSdp.toString();
        
        log.debug("newSdp = " + localSession + "." );
        
        // Finally, we use the "newSdp" and "remoteSdp" to initialize the lasting codec informations.
        CodecUtils.initSipAudioCodec(sipCodec, userProfile.audioDefaultPacketization, 
                userProfile.audioDefaultPacketization, newSdp, remoteSdp);
    }


    /** Callback function called when arriving a 2xx (call accepted) 
     *  The user has managed to join the conference.
     */ 
    public void onCallAccepted(Call call, String sdp, Message resp) {        
    	log.debug("Received 200/OK. So user has successfully joined the conference.");        
    	if (!isCurrentCall(call)) return;
        
        log.debug("ACCEPTED/CALL.");
        callState = CallState.UA_ONCALL;

        setupSdpAndCodec(sdp);

        if (userProfile.noOffer) {
            // Answer with the local sdp.
            call.ackWithAnswer(localSession);
        }

        createVoiceStreams();
    }

    /** Callback function called when arriving an ACK method (call confirmed) */
    public void onCallConfirmed(Call call, String sdp, Message ack) {
    	log.debug("Received ACK. Hmmm...is this for when the server initiates the call????");
        
    	if (!isCurrentCall(call)) return;        
        callState = CallState.UA_ONCALL;
        createVoiceStreams();
    }

    /** Callback function called when arriving a 4xx (call failure) */
    public void onCallRefused(Call call, String reason, Message resp) {        
    	log.debug("Call has been refused.");        
    	if (!isCurrentCall(call)) return;
        callState = CallState.UA_IDLE;
        notifyListenersOnOutgoingCallFailed();
    }

    /** Callback function called when arriving a 3xx (call redirection) */
    public void onCallRedirection(Call call, String reason, Vector contact_list, Message resp) {        
    	log.debug("onCallRedirection");
        
    	if (!isCurrentCall(call)) return;
        call.call(((String) contact_list.elementAt(0)));
    }


    /**
     * Callback function that may be overloaded (extended). Called when arriving a CANCEL request
     */
    public void onCallCanceling(Call call, Message cancel) {
    	log.error("Server shouldn't cancel call...or does it???");
        
    	if (!isCurrentCall(call)) return; 
        
        log.debug("Server has CANCEL-led the call.");
        callState = CallState.UA_IDLE;
        notifyListenersOfOnIncomingCallCancelled();
    }

    private void notifyListenersOnCallConnected(String talkStream, String listenStream) {
    	log.debug("notifyListenersOnCallConnected for {}", clientId);
    	clientConnManager.joinConferenceSuccess(clientId, talkStream, listenStream, sipCodec.getCodecName());
    }
  
    private void notifyListenersOnOutgoingCallFailed() {
    	log.debug("notifyListenersOnOutgoingCallFailed for {}", clientId);
    	clientConnManager.joinConferenceFailed(clientId);
    	cleanup();
    }

    
    private void notifyListenersOfOnIncomingCallCancelled() {
    	log.debug("notifyListenersOfOnIncomingCallCancelled for {}", clientId);
    }
    
    private void notifyListenersOfOnCallClosed() {
    	log.debug("notifyListenersOfOnCallClosed for {}", clientId);
    	clientConnManager.leaveConference(clientId);
    	cleanup();
    }
    
    private void cleanup() {
    	localSocket.close();
    }
    
    /** Callback function called when arriving a BYE request */
    public void onCallClosing(Call call, Message bye) {
    	log.debug("onCallClosing");
        
    	if (!isCurrentCall(call)) return;

        log.debug("CLOSE.");
        
        closeVoiceStreams();

        notifyListenersOfOnCallClosed();
        callState = CallState.UA_IDLE;

        // Reset local sdp for next call.
        initSessionDescriptor();
    }


    /**
     * Callback function called when arriving a response after a BYE request
     * (call closed)
     */
    public void onCallClosed(Call call, Message resp) {
    	log.debug("onCallClosed");
        
    	if (!isCurrentCall(call)) return;         
        log.debug("CLOSE/OK.");
        
        notifyListenersOfOnCallClosed();
        callState = CallState.UA_IDLE;
    }


    /** Callback function called when the invite expires */
    public void onCallTimeout(Call call) {        
    	log.debug("onCallTimeout");
        
    	if (!isCurrentCall(call)) return; 
        
        log.debug("NOT FOUND/TIMEOUT.");
        callState = CallState.UA_IDLE;

        notifyListenersOnOutgoingCallFailed();
    }

    public void onCallStreamStopped() {
    	log.info("Call stream has been stopped");
    	notifyListenersOfOnCallClosed();
    }
    
    private boolean isCurrentCall(Call call) {
    	return this.call == call;
    }
    
    public void setCallStreamFactory(CallStreamFactory csf) {
    	this.callStreamFactory = csf;
    }
    
	public void setClientConnectionManager(ClientConnectionManager ccm) {
		clientConnManager = ccm;
	}
}
