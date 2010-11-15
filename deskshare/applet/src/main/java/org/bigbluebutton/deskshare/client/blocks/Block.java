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
package org.bigbluebutton.deskshare.client.blocks;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bigbluebutton.deskshare.client.net.EncodedBlockData;
import org.bigbluebutton.deskshare.common.PixelExtractException;
import org.bigbluebutton.deskshare.common.ScreenVideoEncoder;
import org.bigbluebutton.deskshare.common.Dimension;

public final class Block {   
	Random random = new Random();
    private final BlockChecksum checksum;
    private final Dimension dim;
    private final int position;
    private final Point location;    
    private int[] capturedPixels;
    private final Object pixelsLock = new Object();
    private AtomicBoolean dirtyBlock = new AtomicBoolean(false);
    private long lastSent = System.currentTimeMillis();
    
    Block(Dimension dim, int position, Point location) {
        checksum = new BlockChecksum();
        this.dim = dim;
        this.position = position;
        this.location = location;
    }
    
    public boolean hasChanged(BufferedImage capturedScreen) {	 
    	synchronized(pixelsLock) {
            try {
            	capturedPixels = ScreenVideoEncoder.getPixels(capturedScreen, getX(), getY(), getWidth(), getHeight());
            } catch (PixelExtractException e) {
            	System.out.println(e.toString());
        	}  
            
            if (! dirtyBlock.get()) {
                if ((! checksumSame(capturedPixels)) || sendKeepAliveBlock()) {
                	dirtyBlock.set(true);
                	return true;
                }            	
            } 
    	}
    	 		    	
        return false;
    }
         
    private boolean isKeepAliveBlock() {
    	// Use block 1 as our keepalive block. The keepalive block is our audit so that the server knows
    	// that the applet is still connected to the server. So it there's no change in the desktop, the applet
    	// should still send this keepalive block.
    	return position == 1;
    }
    
    private boolean sendKeepAliveBlock() {
    	long now = System.currentTimeMillis();
    	if (isKeepAliveBlock() && (now - lastSent > 30000)) {
    		// Send keepalive block every 30 seconds.
    		lastSent = now;
    		System.out.println("Sending keep alive block!");
    		return true;
    	}
    	return false;
    }
    
    public void sent() {
    	dirtyBlock.set(false);
    }
    
    public EncodedBlockData encode() {   
    	int[] pixelsCopy = new int[capturedPixels.length];
    	
    	synchronized (pixelsLock) {     		
            System.arraycopy(capturedPixels, 0, pixelsCopy, 0, capturedPixels.length);
		}
    	
        byte[] encodedBlock = ScreenVideoEncoder.encodePixels(pixelsCopy, getWidth(), getHeight()); 	
        return new EncodedBlockData(position, encodedBlock);		
    }
    
    private boolean checksumSame(int[] pixels) {
    	return checksum.isChecksumSame(convertIntPixelsToBytePixels(pixels)); 
    }
          
    private byte[] convertIntPixelsToBytePixels(int[] pixels) {
    	byte[] p = new byte[pixels.length * 3];
    	int position = 0;
		
		for (int i = 0; i < pixels.length; i++) {
			byte red = (byte) ((pixels[i] >> 16) & 0xff);
			byte green = (byte) ((pixels[i] >> 8) & 0xff);
			byte blue = (byte) (pixels[i] & 0xff);

			// Sequence should be BGR
			p[position++] = blue;
			p[position++] = green;
			p[position++] = red;
		}
		
		return p;
    }
    
    public int getWidth() {
        return new Integer(dim.getWidth()).intValue();
    }
    
    public int getHeight() {
        return new Integer(dim.getHeight()).intValue();
    }
    
    public int getPosition() {
		return new Integer(position).intValue();
	}
    
    public int getX() {
		return new Integer(location.x).intValue();
	}

    public int getY() {
		return new Integer(location.y).intValue();
	}
	
    Dimension getDimension() {
		return new Dimension(dim.getWidth(), dim.getHeight());
	}
	
    Point getLocation() {
		return new Point(location.x, location.y);
	}
}
