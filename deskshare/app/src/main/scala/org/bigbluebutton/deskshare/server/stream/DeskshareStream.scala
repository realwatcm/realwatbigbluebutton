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
package org.bigbluebutton.deskshare.server.stream

import org.bigbluebutton.deskshare.server.red5.DeskshareApplication
import org.bigbluebutton.deskshare.server.ScreenVideoBroadcastStream
import org.red5.server.api.{IContext, IScope}
import org.red5.server.api.so.ISharedObject
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.{BroadcastScope, IBroadcastScope, IProviderService}
import org.apache.mina.core.buffer.IoBuffer
import java.util.ArrayList
import scala.actors.Actor
import scala.actors.Actor._

import net.lag.logging.Logger

class DeskshareStream(app: DeskshareApplication, name: String, val width: Int, val height: Int) extends Stream {
	private val log = Logger.get
	private var broadcastStream:ScreenVideoBroadcastStream = null 

	var startTimestamp: Long = System.currentTimeMillis()
 
	def act() = {
	  loop {
	    react {
	      case StartStream => startStream()
	      case StopStream => stopStream()
	      case us: UpdateStream => updateStream(us)
	      case ml: UpdateStreamMouseLocation => updateStreamMouseLocation(ml)
	      case m:Any => log.warning("DeskshareStream: Unknown message " + m);
	    }
	  }
	}
 
	def initializeStream():Boolean = {
	   app.createScreenVideoBroadcastStream(name) match {
	     case None => return false
	     case Some(bs) => broadcastStream = bs; return true
	   } 
    
	   return false
	}
 
	private def stopStream() = {
		log.debug("DeskShareStream: Stopping stream %s", name)
		log.info("DeskShareStream: Sending deskshareStreamStopped for %s", name)
		broadcastStream.sendDeskshareStreamStopped(new ArrayList[Object]())
		broadcastStream.stop()
	    broadcastStream.close()	  
	    exit()
	}
	
	private def startStream() = {
	  log.debug("DeskShareStream: Starting stream %s", name)
	  
   	  broadcastStream.sendDeskshareStreamStarted(width, height)
	}
	
	private def updateStreamMouseLocation(ml: UpdateStreamMouseLocation) = {
		broadcastStream.sendMouseLocation(ml.loc)
	}
 
	private def updateStream(us: UpdateStream) {
		val buffer: IoBuffer  = IoBuffer.allocate(us.videoData.length, false);
		buffer.put(us.videoData);
		
		/* Set the marker back to zero position so that "gets" start from the beginning.
		 * Otherwise, you get BufferUnderFlowException.
		 */		
		buffer.rewind();	

		val data: VideoData = new VideoData(buffer)
		data.setTimestamp((System.currentTimeMillis() - startTimestamp).toInt)
		broadcastStream.dispatchEvent(data)
		data.release()
		
	}
 
	override def  exit() : Nothing = {
	  log.warning("DeskShareStream: **** Exiting  Actor for room %s", name)
	  super.exit()
	}
 
	override def exit(reason : AnyRef) : Nothing = {
	  log.warning("DeskShareStream: **** Exiting Actor %s for room %s", reason, name)
	  super.exit(reason)
	}
}
