package com.about80minutes.palantir.job.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a response item
 */
public class WikipediaResponseItem {
	/*
	 "title": "Kevin Bacon",
     "text": {
         "*": "<div class=\"hatnote\">For other... "
     }
	 */
	
	/**
	 * The title of the returned article
	 */
	@JsonProperty("title")
	public String title;
	
	/**
	 * article text items
	 */
	@JsonProperty("text")
	public WikipediaResponseItemText text;
}
