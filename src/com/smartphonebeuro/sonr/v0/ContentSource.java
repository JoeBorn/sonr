package com.smartphonebeuro.sonr.v0;

/*
 * 
 * This class pulls metadata from the service if available.
 * 
 *
 */

public abstract class ContentSource
{
	private String displayName;		// Name for service UI shows
	final void setDisplayName(String tempDisplayName)
	{
		displayName = tempDisplayName;
	}
	final String getDisplayName()
	{
		return displayName;
	}
	
	// 
	abstract int nextTrack();
	abstract int previousTrack();
	abstract int pausePlay();
	
	abstract int thumbsUp();
	abstract int thumbsDown();
	abstract int favorite();
	abstract int exit();
	abstract trackMetadata getMetadata();
	
	public class trackMetadata
	{
		boolean exists = false;		//denotes whether or not metadata was available
		String title = "";
		String artist = "";
		String album = "";
		int year = 0;				// 0 denotes data not available
		int trackNumber = 0;		// 0 denotes data no available
		String URL = "";
	}
}