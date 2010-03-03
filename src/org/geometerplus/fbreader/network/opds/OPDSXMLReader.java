/*
 * Copyright (C) 2010 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.network.opds;

import java.util.*;

import org.geometerplus.zlibrary.core.xml.*;

import org.geometerplus.fbreader.constants.XMLNamespace;

import org.geometerplus.fbreader.network.atom.*;


class OPDSXMLReader extends ZLXMLReaderAdapter {

	private final OPDSFeedReader myFeedReader;

	private OPDSFeedMetadata myFeed;
	private OPDSEntry myEntry;

	private ATOMAuthor myAuthor;
	private ATOMId myId;
	private ATOMLink myLink;
	private ATOMCategory myCategory;
	private ATOMUpdated myUpdated;
	private ATOMPublished myPublished;

	//private ATOMTitle myTitle;      // TODO: implement ATOMTextConstruct & ATOMTitle
	//private ATOMSummary mySummary;  // TODO: implement ATOMTextConstruct & ATOMSummary
	private boolean mySummaryTagFound;

	public OPDSXMLReader(OPDSFeedReader feedReader) {
		myFeedReader = feedReader;
	}


	private String myDublinCoreNamespaceId;
	private String myAtomNamespaceId;
	private String myOpenSearchNamespaceId;
	private String myCalibreNamespaceId;

	@Override
	public boolean processNamespaces() {
		return true;
	}

	@Override
	public void namespaceListChangedHandler(HashMap<String,String> namespaces) {
		myDublinCoreNamespaceId = null;
		myAtomNamespaceId = null;
		myOpenSearchNamespaceId = null;
		myCalibreNamespaceId = null;

		for (String key: namespaces.keySet()) {
			if (key.startsWith(XMLNamespace.DublinCoreTermsPrefix)) {
				myDublinCoreNamespaceId = namespaces.get(key).intern();
			} else if (key.equals(XMLNamespace.Atom)) {
				myAtomNamespaceId = namespaces.get(key).intern();
			} else if (key.startsWith(XMLNamespace.OpenSearchPrefix)) {
				myOpenSearchNamespaceId = namespaces.get(key).intern();
			} else if (key.equals(XMLNamespace.CalibreMetadata)) {
				myCalibreNamespaceId = namespaces.get(key).intern();
			}
		}
	}


	private static final int START = 0;
	private static final int FEED = 1;
	private static final int F_ENTRY = 2;
	private static final int F_ID = 3;
	private static final int F_LINK = 4;
	private static final int F_CATEGORY = 5;
	private static final int F_TITLE = 6;
	private static final int F_UPDATED = 7;
	private static final int F_AUTHOR = 8;
	private static final int FA_NAME = 9;
	private static final int FA_URI = 10;
	private static final int FA_EMAIL = 11;
	private static final int FE_AUTHOR = 12;
	private static final int FE_ID = 13;
	private static final int FE_CATEGORY = 14;
	private static final int FE_LINK = 15;
	private static final int FE_PUBLISHED = 16;
	private static final int FE_SUMMARY = 17;
	private static final int FE_CONTENT = 18;
	private static final int FE_SUBTITLE = 19;
	private static final int FE_TITLE = 20;
	private static final int FE_UPDATED = 21;
	private static final int FE_DC_LANGUAGE = 22;
	private static final int FE_DC_ISSUED = 23;
	private static final int FE_DC_PUBLISHER = 24;
	private static final int FEA_NAME = 25;
	private static final int FEA_URI = 26;
	private static final int FEA_EMAIL = 27;
	private static final int OPENSEARCH_TOTALRESULTS = 28;
	private static final int OPENSEARCH_ITEMSPERPAGE = 29;
	private static final int OPENSEARCH_STARTINDEX = 30;

	private static final String TAG_FEED = "feed";
	private static final String TAG_ENTRY = "entry";
	private static final String TAG_AUTHOR = "author";
	private static final String TAG_NAME = "name";
	private static final String TAG_URI = "uri";
	private static final String TAG_EMAIL = "email";
	private static final String TAG_ID = "id";
	private static final String TAG_CATEGORY = "category";
	private static final String TAG_LINK = "link";
	private static final String TAG_PUBLISHED = "published";
	private static final String TAG_SUMMARY = "summary";
	private static final String TAG_CONTENT = "content";
	private static final String TAG_SUBTITLE = "subtitle";
	private static final String TAG_TITLE = "title";
	private static final String TAG_UPDATED = "updated";
	private static final String TAG_META = "meta";

	private static final String DC_TAG_LANGUAGE = "language";
	private static final String DC_TAG_ISSUED = "issued";
	private static final String DC_TAG_PUBLISHER = "publisher";

	private static final String OPENSEARCH_TAG_TOTALRESULTS = "totalResults";
	private static final String OPENSEARCH_TAG_ITEMSPERPAGE = "itemsPerPage";
	private static final String OPENSEARCH_TAG_STARTINDEX = "startIndex";


	int myState = START;

	private final StringBuffer myBuffer = new StringBuffer();


	@Override
	public boolean startElementHandler(String tag, ZLStringMap attributes) {
		String tagPrefix = null;
		final int index = tag.indexOf(':');
		if (index >= 0) {
			tagPrefix = tag.substring(0, index).intern();
			tag = tag.substring(index + 1).intern();
		} else {
			tag = tag.intern();
		}

		switch (myState) {
			case START:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_FEED) {
					myFeedReader.processFeedStart();
					myFeed = new OPDSFeedMetadata();
					myFeed.readAttributes(attributes);
					myState = FEED;
				}
				break;
			case FEED:
				if (tagPrefix == myAtomNamespaceId) {
					if (tag == TAG_AUTHOR) {
						myAuthor = new ATOMAuthor();
						myAuthor.readAttributes(attributes);
						myState = F_AUTHOR;
					} else if (tag == TAG_ID) {
						myId = new ATOMId();
						myId.readAttributes(attributes);
						myState = F_ID;
					} else if (tag == TAG_LINK) {
						myLink = new ATOMLink();
						myLink.readAttributes(attributes);
						myState = F_LINK;
					} else if (tag == TAG_CATEGORY) {
						myCategory = new ATOMCategory();
						myCategory.readAttributes(attributes);
						myState = F_CATEGORY;
					} else if (tag == TAG_TITLE) {
						//myTitle = new ATOMTitle(); // TODO:implement ATOMTextConstruct & ATOMTitle
						//myTitle.readAttributes(attributes);
						myState = F_TITLE;
					} else if (tag == TAG_UPDATED) {
						myUpdated = new ATOMUpdated();
						myUpdated.readAttributes(attributes);
						myState = F_UPDATED;
					} else if (tag == TAG_ENTRY) {
						myEntry = new OPDSEntry();
						myEntry.readAttributes(attributes);
						mySummaryTagFound = false;
						myState = F_ENTRY;
					} 
				} else if (tagPrefix == myOpenSearchNamespaceId) {
					if (tag == OPENSEARCH_TAG_TOTALRESULTS) {
						myState = OPENSEARCH_TOTALRESULTS;
					} else if (tag == OPENSEARCH_TAG_ITEMSPERPAGE) {
						myState = OPENSEARCH_ITEMSPERPAGE;
					} else if (tag == OPENSEARCH_TAG_STARTINDEX) {
						myState = OPENSEARCH_STARTINDEX;
					} 
				} 
				break;
			case F_ENTRY:
				if (tagPrefix == myAtomNamespaceId) {
					if (tag == TAG_AUTHOR) {
						myAuthor = new ATOMAuthor();
						myAuthor.readAttributes(attributes);
						myState = FE_AUTHOR;
					} else if (tag == TAG_ID) {
						myId = new ATOMId();
						myId.readAttributes(attributes);
						myState = FE_ID;
					} else if (tag == TAG_CATEGORY) {
						myCategory = new ATOMCategory();
						myCategory.readAttributes(attributes);
						myState = FE_CATEGORY;
					} else if (tag == TAG_LINK) {
						myLink = new ATOMLink();
						myLink.readAttributes(attributes);
						myState = FE_LINK;
					} else if (tag == TAG_PUBLISHED) {
						myPublished = new ATOMPublished();
						myPublished.readAttributes(attributes);
						myState = FE_PUBLISHED;
					} else if (tag == TAG_SUMMARY) {
						//mySummary = new ATOMSummary(); // TODO:implement ATOMTextConstruct & ATOMSummary
						//mySummary.readAttributes(attributes);
						myState = FE_SUMMARY;
					} else if (tag == TAG_CONTENT) {
						// ???
						myState = FE_CONTENT;
					} else if (tag == TAG_SUBTITLE) {
						// ???
						myState = FE_SUBTITLE;
					} else if (tag == TAG_TITLE) {
						//myTitle = new ATOMTitle(); // TODO:implement ATOMTextConstruct & ATOMTitle
						//myTitle.readAttributes(attributes);
						myState = FE_TITLE;
					} else if (tag == TAG_UPDATED) {
						myUpdated = new ATOMUpdated();
						myUpdated.readAttributes(attributes);
						myState = FE_UPDATED;
					} else if (tag == TAG_META) {
						String name = attributes.getValue("name");
						String value = attributes.getValue("value");
						if (name != null && value != null) {
							if (name.equals(myCalibreNamespaceId + ":series")) {
								myEntry.SeriesTitle = value;
							} else if (name.equals(myCalibreNamespaceId + ":series_index")) {
								try {
									myEntry.SeriesIndex = Integer.parseInt(value);
								} catch (NumberFormatException ex) {
								}
							}
						}
					}
				} else if (tagPrefix == myDublinCoreNamespaceId) {
					if (tag == DC_TAG_LANGUAGE) {
						myState = FE_DC_LANGUAGE;
					} else if (tag == DC_TAG_ISSUED) {
						myState = FE_DC_ISSUED;
					} else if (tag == DC_TAG_PUBLISHER) {
						myState = FE_DC_PUBLISHER;
					} 
				}
				break;
			case F_AUTHOR:
				if (tagPrefix == myAtomNamespaceId) {
					if (tag == TAG_NAME) {
						myState = FA_NAME;
					} else if (tag == TAG_URI) {
						myState = FA_URI;
					} else if (tag == TAG_EMAIL) {
						myState = FA_EMAIL;
					} 
				} 
				break;
			case FE_AUTHOR:
				if (tagPrefix == myAtomNamespaceId) {
					if (tag == TAG_NAME) {
						myState = FEA_NAME;
					} else if (tag == TAG_URI) {
						myState = FEA_URI;
					} else if (tag == TAG_EMAIL) {
						myState = FEA_EMAIL;
					} 
				}
				break;
			default:
				break;
		}

		myBuffer.delete(0, myBuffer.length());
		return false;
	}

	@Override
	public boolean endElementHandler(String tag) {
		String tagPrefix = null;
		final int index = tag.indexOf(':');
		if (index >= 0) {
			tagPrefix = tag.substring(0, index).intern();
			tag = tag.substring(index + 1).intern();
		} else {
			tag = tag.intern();
		}

		final String bufferContent = myBuffer.toString().trim();
		myBuffer.delete(0, myBuffer.length());

		switch (myState) {
			case START:
				break;
			case FEED:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_FEED) {
					myFeedReader.processFeedMetadata(myFeed);
					myFeed = null;
					myFeedReader.processFeedEnd();
					myState = START;
				} 
				break;
			case F_ENTRY:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_ENTRY) {
					myFeedReader.processFeedEntry(myEntry);
					myEntry = null;
					myState = FEED;
				}
				break;
			case F_ID:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_ID) {
					// FIXME:uri can be lost:buffer will be truncated, if there are extension tags inside the <id> tag
					myId.Uri = bufferContent;
					if (myFeed != null) {
						myFeed.Id = myId;
					}
					myId = null;
					myState = FEED;
				} 
				break;
			case F_LINK:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_LINK) {
					if (myFeed != null) {
						myFeed.Links.add(myLink);
					}
					myLink = null;
					myState = FEED;
				} 
				break;
			case F_CATEGORY:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_CATEGORY) {
					if (myFeed != null) {
						myFeed.Categories.add(myCategory);
					}
					myCategory = null;
					myState = FEED;
				} 
				break;
			case F_TITLE:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_TITLE) {
					// FIXME:title can be lost:buffer will be truncated, if there are extension tags inside the <title> tag
					// TODO:implement ATOMTextConstruct & ATOMTitle
					if (myFeed != null) {
						myFeed.Title = bufferContent;
					}
					myState = FEED;
				} 
				break;
			case F_UPDATED:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_UPDATED) {
					// FIXME:uri can be lost:buffer will be truncated, if there are extension tags inside the <id> tag
					ATOMDateConstruct.parse(bufferContent, myUpdated);
					if (myFeed != null) {
						myFeed.Updated = myUpdated;
					}
					myUpdated = null;
					myState = FEED;
				} 
				break;
			case F_AUTHOR:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_AUTHOR) {
					if (myFeed != null) {
						myFeed.Authors.add(myAuthor);
					}
					myAuthor = null;
					myState = FEED;
				} 
				break;
			case FA_NAME:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_NAME) {
					myAuthor.Name = bufferContent;
					myState = F_AUTHOR;
				}
				break;
			case FEA_NAME:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_NAME) {
					myAuthor.Name = bufferContent;
					myState = FE_AUTHOR;
				}
				break;
			case FA_URI:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_URI) {
					myAuthor.Uri = bufferContent;
					myState = F_AUTHOR;
				}
				break;
			case FEA_URI:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_URI) {
					myAuthor.Uri = bufferContent;
					myState = FE_AUTHOR;
				}
				break;
			case FA_EMAIL:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_EMAIL) {
					myAuthor.Email = bufferContent;
					myState = F_AUTHOR;
				}
				break;
			case FEA_EMAIL:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_EMAIL) {
					myAuthor.Email = bufferContent;
					myState = FE_AUTHOR;
				}
				break;
			case FE_AUTHOR:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_AUTHOR) {
					myEntry.Authors.add(myAuthor);
					myAuthor = null;
					myState = F_ENTRY;
				} 
				break;
			case FE_ID:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_ID) {
					// FIXME:uri can be lost:buffer will be truncated, if there are extension tags inside the <id> tag
					myId.Uri = bufferContent;
					myEntry.Id = myId;
					myId = null;
					myState = F_ENTRY;
				}
				break;
			case FE_CATEGORY:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_CATEGORY) {
					myEntry.Categories.add(myCategory);
					myCategory = null;
					myState = F_ENTRY;
				}
				break;
			case FE_LINK:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_LINK) {
					myEntry.Links.add(myLink);
					myLink = null;
					myState = F_ENTRY;
				}
				break;
			case FE_PUBLISHED:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_PUBLISHED) {
					// FIXME:uri can be lost:buffer will be truncated, if there are extension tags inside the <id> tag
					ATOMDateConstruct.parse(bufferContent, myPublished);
					myEntry.Published = myPublished;
					myPublished = null;
					myState = F_ENTRY;
				}
				break;
			case FE_SUMMARY:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_SUMMARY) {
					// FIXME:summary can be lost:buffer will be truncated, if there are extension tags inside the <summary> tag
					// TODO:implement ATOMTextConstruct & ATOMSummary
					myEntry.Summary = bufferContent;
					mySummaryTagFound = true;
					myState = F_ENTRY;
				}
				break;
			case FE_CONTENT:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_CONTENT) {
					// TODO:check this accurately
					if (!mySummaryTagFound) {
						myEntry.Summary = bufferContent;
					}
					myState = F_ENTRY;
				}
				break;
			case FE_SUBTITLE:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_SUBTITLE) {
					// TODO:check this accurately
					if (!mySummaryTagFound) {
						myEntry.Summary = bufferContent;
					}
					myState = F_ENTRY;
				}
				break;
			case FE_TITLE:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_TITLE) {
					// FIXME:title can be lost:buffer will be truncated, if there are extension tags inside the <title> tag
					// TODO:implement ATOMTextConstruct & ATOMTitle
					myEntry.Title = bufferContent;
					myState = F_ENTRY;
				}
				break;
			case FE_UPDATED:
				if (tagPrefix == myAtomNamespaceId && tag == TAG_UPDATED) {
					// FIXME:uri can be lost:buffer will be truncated, if there are extension tags inside the <id> tag
					ATOMDateConstruct.parse(bufferContent, myUpdated);
					myEntry.Updated = myUpdated;
					myUpdated = null;
					myState = F_ENTRY;
				}
				break;
			case FE_DC_LANGUAGE:
				if (tagPrefix == myDublinCoreNamespaceId && tag == DC_TAG_LANGUAGE) {
					// FIXME:language can be lost:buffer will be truncated, if there are extension tags inside the <dc:language> tag
					myEntry.DCLanguage = bufferContent;
					myState = F_ENTRY;
				}
				break;
			case FE_DC_ISSUED:
				if (tagPrefix == myDublinCoreNamespaceId && tag == DC_TAG_ISSUED) {
					// FIXME:issued can be lost:buffer will be truncated, if there are extension tags inside the <dc:issued> tag
					DCDate issued = new DCDate();
					ATOMDateConstruct.parse(bufferContent, issued);
					myEntry.DCIssued = issued;
					myState = F_ENTRY;
				}
				break;
			case FE_DC_PUBLISHER:
				if (tagPrefix == myDublinCoreNamespaceId && tag == DC_TAG_PUBLISHER) {
					// FIXME:publisher can be lost:buffer will be truncated, if there are extension tags inside the <dc:publisher> tag
					myEntry.DCPublisher = bufferContent;
					myState = F_ENTRY;
				}
				break;
			case OPENSEARCH_TOTALRESULTS:
				if (tagPrefix == myOpenSearchNamespaceId &&
						tag == OPENSEARCH_TAG_TOTALRESULTS) {
					try {
						int number = Integer.parseInt(bufferContent);
						if (myFeed != null) {
							myFeed.OpensearchTotalResults = number;
						}
					} catch (NumberFormatException ex) {
					}
					myState = FEED;
				}
				break;
			case OPENSEARCH_ITEMSPERPAGE:
				if (tagPrefix == myOpenSearchNamespaceId &&
						tag == OPENSEARCH_TAG_ITEMSPERPAGE) {
					try {
						int number = Integer.parseInt(bufferContent);
						if (myFeed != null) {
							myFeed.OpensearchItemsPerPage = number;
						}
					} catch (NumberFormatException ex) {
					}
					myState = FEED;
				}
				break;
			case OPENSEARCH_STARTINDEX:
				if (tagPrefix == myOpenSearchNamespaceId &&
						tag == OPENSEARCH_TAG_STARTINDEX) {
					try {
						int number = Integer.parseInt(bufferContent);
						if (myFeed != null) {
							myFeed.OpensearchStartIndex = number;
						}
					} catch (NumberFormatException ex) {
					}
					myState = FEED;
				}
				break;
		}

		return false;
	}

	@Override
	public void characterDataHandler(char[] data, int start, int length) {
		myBuffer.append(data, start, length);
	}
}