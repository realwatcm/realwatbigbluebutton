/**
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
*/
package org.bigbluebutton.modules.present.business
{
	import com.asfusion.mate.events.Dispatcher;
	
	import flash.events.*;
	import flash.net.FileReference;
	import flash.net.URLRequest;
	import flash.net.URLRequestMethod;
	import flash.net.URLVariables;	
	import org.bigbluebutton.modules.present.events.UploadEvent;
	import org.bigbluebutton.common.LogUtil;
	
	public class FileUploadService {
		public static const ID:String = "FileUploadService";

		public static const UPLOAD_PROGRESS:String = "UPLOAD_PROGRESS";
		public static const UPLOAD_COMPLETED:String = "UPLOAD_COMPLETED";
		public static const UPLOAD_IO_ERROR:String = "UPLOAD_IO_ERROR";
		public static const UPLOAD_SECURITY_ERROR:String = "UPLOAD_SECURITY_ERROR";
		
		private var request:URLRequest = new URLRequest();
		private var sendVars:URLVariables = new URLVariables();
		
		private var dispatcher:Dispatcher;
		
		/**
		 * The default constructor 
		 * @param url - the address of the server
		 * @param room - a room in the server we're connecting to
		 * 
		 */		
		public function FileUploadService(url:String, conference:String, room:String):void {
			sendVars.conference = conference;
			sendVars.room = room;
			request.url = url;
			request.data = sendVars;
			dispatcher = new Dispatcher();
		}

		/**
		 * Uploads local files to a server 
		 * @param file - The FileReference class of the file we wish to send
		 * 
		 */		
		public function upload(presentationName:String, file:FileReference):void {
			sendVars.presentation_name = presentationName;
			var fileToUpload : FileReference = new FileReference();
			fileToUpload = file;
			
			fileToUpload.addEventListener(ProgressEvent.PROGRESS, onUploadProgress);
			fileToUpload.addEventListener(Event.COMPLETE, onUploadComplete);
			fileToUpload.addEventListener(IOErrorEvent.IO_ERROR, onUploadIoError);
			fileToUpload.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onUploadSecurityError);
			fileToUpload.addEventListener(HTTPStatusEvent.HTTP_STATUS, httpStatusHandler);
			fileToUpload.addEventListener(Event.OPEN, openHandler);

			request.method = URLRequestMethod.POST;
			
			// "fileUpload" is the variable name of the uploaded file in the server
			fileToUpload.upload(request, "fileUpload", true);
		}
		
		private function httpStatusHandler(event:HTTPStatusEvent):void {
			// TO CLEANUP
			//_progressListener(PresentModuleConstants.UPLOAD_IO_ERROR_EVENT, "HTTP STATUS EVENT");
        	}

		private function openHandler(event:Event):void {
			// TO CLEANUP
			//_progressListener(PresentModuleConstants.UPLOAD_IO_ERROR_EVENT, "OPEN HANDLER");
        	}


		/**
		 * Receives an ProgressEvent which then updated the progress bar on the view 
		 * @param event - a ProgressEvent
		 * 
		 */		
		private function onUploadProgress(event:ProgressEvent) : void {
			var percentage:Number = Math.round((event.bytesLoaded / event.bytesTotal) * 100);
			var e:UploadEvent = new UploadEvent(UploadEvent.UPLOAD_PROGRESS_UPDATE);
			e.percentageComplete = percentage;
			dispatcher.dispatchEvent(e);
		}
		
		/**
		 * Method is called when the upload has completed successfuly 
		 * @param event
		 * 
		 */		
		private function onUploadComplete(event:Event):void {
			dispatcher.dispatchEvent(new UploadEvent(UploadEvent.UPLOAD_COMPLETE));
		}

		/**
		 * Receives an ErrorEvent when an error occured during the upload 
		 * @param event
		 * 
		 */
		private function onUploadIoError(event:IOErrorEvent):void {
			dispatcher.dispatchEvent(new UploadEvent(UploadEvent.UPLOAD_IO_ERROR));
			LogUtil.error("An error occured while uploading the file. " + event.toString()); 
		}
		
		/**
		 * Method is called when a SecurityError is received 
		 * @param event
		 * 
		 */		
		private function onUploadSecurityError(event:SecurityErrorEvent) : void {
			dispatcher.dispatchEvent(new UploadEvent(UploadEvent.UPLOAD_SECURITY_ERROR));
			LogUtil.error("A security error occured while trying to upload the presentation. " + event.toString());
		}		
	}
}