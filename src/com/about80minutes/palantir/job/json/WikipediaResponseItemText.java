package com.about80minutes.palantir.job.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * article text from wikipedia
 */
public class WikipediaResponseItemText {
	/*
	 "text": {
         "*": "<div class=\"hatnote\">For other... "
     }
	 */
	
	/**
	 * article text.
	 */
	@JsonProperty("*")
	public String articleText;
}
