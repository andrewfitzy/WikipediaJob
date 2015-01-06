package com.about80minutes.palantir.job;

import java.util.Collection;

import org.apache.hadoop.thirdparty.guava.common.collect.Lists;

import com.palantir.services.ptobject.Link;
import com.palantir.services.ptobject.PTObjectContainer;

/**
 * Encapsulates the results of doing a download task.
 */
public class WikipediaDownloadResult {
	public Collection<PTObjectContainer> children = Lists.newArrayList();
	public Collection<Link> links = Lists.newArrayList();
	public PTObjectContainer parent = null;
}
