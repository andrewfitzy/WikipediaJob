package com.about80minutes.palantir.job.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a search response from Wikipedia
 */
public class WikipediaResponse {
	/*
	 {
	     "parse": {
	         "title": "Kevin Bacon",
	         "text": {
	             "*": "<div class=\"hatnote\">For other... "
	         }
	     }
	}
	*/
	
	/**
	 * The search response
	 */
	@JsonProperty("parse")
	public WikipediaResponseItem responseItem;
}
