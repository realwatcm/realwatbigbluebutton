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
package org.bigbluebutton.presentation.imp

import org.springframework.util.FileCopyUtils
import org.bigbluebutton.presentation.Util

class Pdf2SwfPageCounterTests extends GroovyTestCase {

	def pageCounter
	def SWFTOOLS_DIR = '/bin'
	static final String PRESENTATIONDIR = '/tmp/var/bigbluebutton'

	def conf = "test-conf"
	def rm = "test-room"
	def presName = "thumbs"	
	def presDir
			
	void setUp() {
		println "Test setup"
		pageCounter = new Pdf2SwfPageCounter()		
		pageCounter.setSwfToolsDir(SWFTOOLS_DIR)	
		
		presDir = new File("$PRESENTATIONDIR/$conf/$rm/$presName")
		if (presDir.exists()) Util.deleteDirectory(presDir)
		
		presDir.mkdirs()
	}
	
	
    void testCountPresentationPages() {
		def uploadedFilename = 'SeveralBigPagesPresentation.pdf'		
		def uploadedFile = new File("test/resources/$uploadedFilename")
			
		def uploadedPresentation = new File(presDir.absolutePath + "/$uploadedFilename")
		int copied = FileCopyUtils.copy(uploadedFile, uploadedPresentation) 
		assertTrue(uploadedPresentation.exists())
				
		int numPages = pageCounter.countNumberOfPages(uploadedPresentation)
   	
		assertEquals 5, numPages
    }
}
