package org.red5.app.sip.stream;

import org.red5.app.sip.AudioStream;
import org.red5.app.sip.trancoders.TranscodedAudioDataListener;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.stream.BroadcastScope;
import org.red5.server.stream.IBroadcastScope;
import org.red5.server.stream.IProviderService;
import org.slf4j.Logger;

public class ListenStream implements TranscodedAudioDataListener {
	final private Logger log = Red5LoggerFactory.getLogger(ListenStream.class, "sip");
		
	private AudioStream broadcastStream;
	private IScope scope;
	private final String listenStreamName;

	public ListenStream(IScope scope) {
		this.scope = scope;
		listenStreamName = "speaker_" + System.currentTimeMillis();
		scope.setName(listenStreamName);	
	}
	
	public String getStreamName() {
		return listenStreamName;
	}
	
	public void stop() {
		streamEnded();
	}
	
	public void start() {
		System.out.println("**** Starting listen stream ****");
		startPublishing(scope);
	}
	
	public void handleTranscodedAudioData(AudioData audioData) {
		streamAudioData(audioData);
	}
	
	private void streamAudioData(AudioData audioData) {
		long startRx = System.currentTimeMillis();
		
		/*
		 * Don't set the timestamp as it results in choppy audio. Let the client
		 * play the audio as soon as they receive the packets. (ralam dec 10, 2009)
		 */
		broadcastStream.dispatchEvent(audioData);
		audioData.release();
		long completeRx = System.currentTimeMillis();
//		System.out.println("Send took " + (completeRx - startRx) + "ms.");
	}

	private void streamEnded() {
		broadcastStream.stop();
	    broadcastStream.close();
	    log.debug("stopping and closing stream {}", listenStreamName);
	}
	
	private void startPublishing(IScope aScope){
		System.out.println("started publishing stream in " + aScope.getName());

		broadcastStream = new AudioStream(listenStreamName);
		broadcastStream.setPublishedName(listenStreamName);
		broadcastStream.setScope(aScope);
		
		IContext context = aScope.getContext();
		
		IProviderService providerService = (IProviderService) context.getBean(IProviderService.BEAN_NAME);
		if (providerService.registerBroadcastStream(aScope, listenStreamName, broadcastStream)){
			IBroadcastScope bScope = (BroadcastScope) providerService.getLiveProviderInput(aScope, listenStreamName, true);
			
			bScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, broadcastStream);
		} else{
			log.error("could not register broadcast stream");
			throw new RuntimeException("could not register broadcast stream");
		}
	    
	    broadcastStream.start();
	}
}
