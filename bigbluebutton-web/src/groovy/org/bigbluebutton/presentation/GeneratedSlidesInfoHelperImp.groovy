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
 * Author: Richard Alam <ritzalam@gmail.com>
 * 		   DJP <DJP@architectes.org>
 * 
 * @version $Id: $
 */
package org.bigbluebutton.presentation

/*
 * Helper class to get info about the generated slides. Easier to
 * generate XML in Groovy.
 */
public class GeneratedSlidesInfoHelperImp implements GeneratedSlidesInfoHelper {

	 /*
	  * Returns an XML string containing the URL for the slides and thumbails.
	  */
	public String generateUploadedPresentationInfo(UploadedPresentation pres) {
		def writer = new java.io.StringWriter()
		def builder = new groovy.xml.MarkupBuilder(writer)
		        		
		def uploadedpresentation = builder.uploadedpresentation {        
		    conference(id:pres.conference, room:pres.room) {
		       presentation(name:pres.name) {
		          slides(count:pres.numberOfPages) {
		             for (def i = 1; i <= pres.numberOfPages; i++) {
		                slide(number:"${i}", name:"slide/${i}", thumb:"thumbnail/${i}")
		             }
		          }
		       }
			}
		}
	
		return writer.toString()		
	}
	
}
