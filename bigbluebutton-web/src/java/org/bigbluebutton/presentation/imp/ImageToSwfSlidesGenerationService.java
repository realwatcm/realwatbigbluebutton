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
package org.bigbluebutton.presentation.imp;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bigbluebutton.presentation.PageConverter;
import org.bigbluebutton.presentation.ImageToSwfSlide;
import org.bigbluebutton.presentation.ThumbnailCreator;
import org.bigbluebutton.presentation.UploadedPresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageToSwfSlidesGenerationService {
	private static Logger log = LoggerFactory.getLogger(ImageToSwfSlidesGenerationService.class);
	
	private ExecutorService executor;
	private CompletionService<ImageToSwfSlide> completionService;
	
	private SwfSlidesGenerationProgressNotifier notifier;
	private PageConverter jpgToSwfConverter;
	private PageConverter pngToSwfConverter;
	private ThumbnailCreator thumbnailCreator;
	private long MAX_CONVERSION_TIME = 5*60*1000;
	private String BLANK_SLIDE;
	
	public ImageToSwfSlidesGenerationService() {
		int numThreads = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(numThreads);
		completionService = new ExecutorCompletionService<ImageToSwfSlide>(executor);
	}
	
	public void generateSlides(UploadedPresentation pres) {
		log.debug("Generating slides");	
		pres.setNumberOfPages(1); // There should be only one image to convert.
		log.debug("Determined number of pages " + pres.getNumberOfPages());
		if (pres.getNumberOfPages() > 0) {
			PageConverter pageConverter = determinePageConverter(pres);
			convertImageToSwf(pres, pageConverter);
		}
		
		
		createThumbnails(pres);
		
		notifier.sendConversionCompletedMessage(pres);
	}
	
	private PageConverter determinePageConverter(UploadedPresentation pres) {
		String fileType = pres.getFileType().toUpperCase();
		if (("JPEG".equals(fileType)) || ("JPG".equals(fileType))) {
			return jpgToSwfConverter;
		}
		
		return pngToSwfConverter;
	}
	
	private void createThumbnails(UploadedPresentation pres) {
		log.debug("Creating thumbnails.");
		notifier.sendCreatingThumbnailsUpdateMessage(pres);
		thumbnailCreator.createThumbnails(pres.getUploadedFile(), pres.getNumberOfPages());
	}
	
	private void convertImageToSwf(UploadedPresentation pres, PageConverter pageConverter) {
		int numPages = pres.getNumberOfPages();				
		ImageToSwfSlide[] slides = setupSlides(pres, numPages, pageConverter);
		generateSlides(slides);		
		handleSlideGenerationResult(pres, slides);		
	}
	
	private void handleSlideGenerationResult(UploadedPresentation pres, ImageToSwfSlide[] slides) {
		long endTime = System.currentTimeMillis() + MAX_CONVERSION_TIME;
		int slideGenerated = 0;
		
		for (int t = 0; t < slides.length; t++) {
			Future<ImageToSwfSlide> future = null;
			ImageToSwfSlide slide = null;
			try {
				long timeLeft = endTime - System.currentTimeMillis();
				future = completionService.take();
				slide = future.get(timeLeft, TimeUnit.MILLISECONDS);
				System.out.println("handleSlideGenerationResult " + slide.getPageNumber());
			} catch (InterruptedException e) {
				log.error("InterruptedException while creating slide " + pres.getName());
			} catch (ExecutionException e) {
				log.error("ExecutionException while creating slide " + pres.getName());
			} catch (TimeoutException e) {
				log.error("TimeoutException while converting " + pres.getName());				
			} finally {
				if ((slide != null) && (! slide.isDone())){
					log.warn("Creating blank slide for " + slide.getPageNumber());
					future.cancel(true);
					slide.generateBlankSlide();
				}
			}
			slideGenerated++;	
			notifier.sendConversionUpdateMessage(slideGenerated, pres);
		}
	}
	
	private ImageToSwfSlide[] setupSlides(UploadedPresentation pres, int numPages, PageConverter pageConverter) {
		ImageToSwfSlide[] slides = new ImageToSwfSlide[numPages];
		
		for (int page = 1; page <= numPages; page++) {		
			ImageToSwfSlide slide = new ImageToSwfSlide(pres, page);
			slide.setBlankSlide(BLANK_SLIDE);
			slide.setPageConverter(pageConverter);
			
			// Array index is zero-based
			slides[page-1] = slide;
		}
		
		return slides;
	}
	
	private void generateSlides(ImageToSwfSlide[] slides) {
		for (int i = 0; i < slides.length; i++) {
			System.out.println("generateSlides " + i);
			final ImageToSwfSlide slide = slides[i];
			completionService.submit(new Callable<ImageToSwfSlide>() {
				public ImageToSwfSlide call() {
					return slide.createSlide();
				}
			});
		}
	}
		
	public void setJpgPageConverter(PageConverter converter) {
		this.jpgToSwfConverter = converter;
	}
	
	public void setPngPageConverter(PageConverter converter) {
		this.pngToSwfConverter = converter;
	}
	
	public void setBlankSlide(String blankSlide) {
		this.BLANK_SLIDE = blankSlide;
	}
	
	public void setThumbnailCreator(ThumbnailCreator thumbnailCreator) {
		this.thumbnailCreator = thumbnailCreator;
	}
	
	public void setMaxConversionTime(int minutes) {
		MAX_CONVERSION_TIME = minutes * 60 * 1000;
	}
	
	public void setSwfSlidesGenerationProgressNotifier(SwfSlidesGenerationProgressNotifier notifier) {
		this.notifier = notifier;
	}
}
