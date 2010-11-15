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
package org.bigbluebutton.voiceconf.red5.media;

import org.bigbluebutton.voiceconf.red5.media.transcoder.FlashToSipTranscoder;
import org.bigbluebutton.voiceconf.red5.media.transcoder.NellyFlashToSipTranscoderImp;
import org.bigbluebutton.voiceconf.red5.media.transcoder.NellySipToFlashTranscoderImp;
import org.bigbluebutton.voiceconf.red5.media.transcoder.SipToFlashTranscoder;
import org.bigbluebutton.voiceconf.red5.media.transcoder.SpeexFlashToSipTranscoderImp;
import org.bigbluebutton.voiceconf.red5.media.transcoder.SpeexSipToFlashTranscoderImp;
import org.bigbluebutton.voiceconf.sip.SipConnectInfo;
import org.red5.app.sip.codecs.Codec;
import org.red5.app.sip.codecs.SpeexCodec;
import org.slf4j.Logger;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;

public class CallStream implements StreamObserver {
    private final static Logger log = Red5LoggerFactory.getLogger(CallStream.class, "sip");

    private FlashToSipAudioStream userTalkStream;
    private SipToFlashAudioStream userListenStream;
    private final Codec sipCodec;
    private final SipConnectInfo connInfo;
    private final IScope scope;
    private CallStreamObserver callStreamObserver;
    
    public CallStream(Codec sipCodec, SipConnectInfo connInfo, IScope scope) {        
    	this.sipCodec = sipCodec;
    	this.connInfo = connInfo;
    	this.scope = scope;
    }
    
    public void addCallStreamObserver(CallStreamObserver observer) {
    	callStreamObserver = observer;
    }
    
    public void start() {        
    	SipToFlashTranscoder sipToFlashTranscoder = new SpeexSipToFlashTranscoderImp(sipCodec);
    	FlashToSipTranscoder flashToSipTranscoder = new SpeexFlashToSipTranscoderImp(sipCodec);

		if (sipCodec.getCodecId() != SpeexCodec.codecId) {			
			flashToSipTranscoder = new NellyFlashToSipTranscoderImp(sipCodec);
			sipToFlashTranscoder = new NellySipToFlashTranscoderImp(sipCodec);
		} 
		
		log.info("Using codec=" + sipCodec.getCodecName() + " id=" + sipCodec.getCodecId());
		log.debug("Packetization [" + sipCodec.getIncomingPacketization() + "," + sipCodec.getOutgoingPacketization() + "]");
		log.debug("Outgoing Frame size [" + sipCodec.getOutgoingEncodedFrameSize() + ", " + sipCodec.getOutgoingDecodedFrameSize() + "]");
		log.debug("Incoming Frame size [" + sipCodec.getIncomingEncodedFrameSize() + ", " + sipCodec.getIncomingDecodedFrameSize() + "]");

		userListenStream = new SipToFlashAudioStream(scope, sipToFlashTranscoder, connInfo.getSocket());
		userListenStream.addListenStreamObserver(this);	
		userTalkStream = new FlashToSipAudioStream(flashToSipTranscoder, connInfo.getSocket(), connInfo); 
    }
    
    public String getTalkStreamName() {
    	return userTalkStream.getStreamName();
    }
    
    public String getListenStreamName() {
    	return userListenStream.getStreamName();
    }
    
    public void startTalkStream(IBroadcastStream broadcastStream, IScope scope) throws StreamException {
    	userListenStream.start();
    	userTalkStream.start(broadcastStream, scope);
    }
    
    public void stopTalkStream(IBroadcastStream broadcastStream, IScope scope) {
    	userTalkStream.stop(broadcastStream, scope);
    }

    public void stop() {
        userListenStream.stop();
    }

	@Override
	public void onStreamStopped() {
		log.debug("STREAM HAS STOPPED " + connInfo.getSocket().getLocalPort());
		if (callStreamObserver != null) callStreamObserver.onCallStreamStopped();
	}
}
