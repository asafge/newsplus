package com.noinnion.android.newsplus.extension.cnn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.internal.IItemIdListHandler;
import com.noinnion.android.reader.api.internal.IItemListHandler;
import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
import com.noinnion.android.reader.api.internal.ITagListHandler;
import com.noinnion.android.reader.api.provider.IItem;
import com.noinnion.android.reader.api.provider.ISubscription;
import com.noinnion.android.reader.api.provider.ITag;

public class CnnExtension extends ReaderExtension {
	
	// {"CAT:Politics", "Politics"}
	public ArrayList<String[]> CATEGORIES;
		
	// {"FEED:http://www.newsblur.com/reader/feed/1818:id", "Coding horror", "http://www.codinghorror.com/blog/", "Politics"}
	public ArrayList<String[]> FEEDS;
	
	/*
	 * Get the categories (folders) and their feeds
	 * 
	 * API call: http://www.newsblur.com/reader/feeds
	 * Result: folders/0/Math/[ID] (ID = 1818)
	 */
	private void getCategoriesAndFeeds() {
		String url = "http://www.newsblur.com/reader/feeds?flat=true";
		final AQuery aq = new AQuery(this);

		AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
			@Override
			public void callback(String url, JSONObject json, AjaxStatus status) {
				try {
					if (json != null) {
						JSONArray folders = json.getJSONArray("flat_folders");
						CATEGORIES = new ArrayList<String[]>();
						JSONObject feeds = json.getJSONObject("feeds");
						FEEDS = new ArrayList<String[]>();					
						if (folders.length() > 0)
						{
							for (int i=0; i<folders.length(); i++) {
								JSONObject cat = folders.getJSONObject(i);
								String catName = cat.keys().next().toString();
								String[] categoryItem = { "CAT:" + catName, catName };
								CATEGORIES.add(categoryItem);
								JSONArray feedsPerFolder = cat.getJSONArray(catName);
								for (int j=0; j<feedsPerFolder.length(); j++) {
									String feedID = feedsPerFolder.getString(j);
									JSONObject f = (JSONObject)feeds.get(feedID);
									String feedUID = "FEED:http://www.newsblur.com/reader/feed/" + feedID + ":id";
									String feedTitle = f.getString("feed_title");
									String feedHtmlUrl = f.getString("feed_link");
									if (catName != "")
										catName = "CAT:" + catName;
									String[] feedItem = {feedUID, feedTitle, feedHtmlUrl, catName};
									FEEDS.add(feedItem);
								}
							}
						}
					}
					else
					{
						status.getCode();		// TODO: Check for 403
					}
				}
				catch (Exception e) {
					AQUtility.report(e);
				}
			}
		};
		cb.header("User-Agent", System.getProperty("http.agent"));
		aq.ajax(url, JSONObject.class, cb);
	}

	@Override
	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException {
		List<ITag> tags = new ArrayList<ITag>();
		List<ISubscription> feeds = new ArrayList<ISubscription>();
		
		getCategoriesAndFeeds();		// TODO: Is this the right place to call?
		
		try {
			for (String[] cat : CATEGORIES) {
				ITag tag = new ITag();
				tag.uid = cat[0];
				tag.label = cat[1];
				if (tag.uid.startsWith("LABEL")) tag.type = ITag.TYPE_TAG_LABEL;
				else if (tag.uid.startsWith("CAT")) tag.type = ITag.TYPE_FOLDER;
				tags.add(tag);
			}
			for (String[] feed : FEEDS) {
				ISubscription sub = new ISubscription();
				sub.uid = feed[0];
				sub.title = feed[1];
				sub.htmlUrl = feed[2];
				if (!TextUtils.isEmpty(feed[3])) {
					sub.addCategory(feed[3]);
				}
				feeds.add(sub);
			}
			
			tagHandler.tags(tags);
			subHandler.subscriptions(feeds);
		}
		catch (RemoteException e) {
			throw new ReaderException("remote connection error", e);			
		}
	}
	
	@Override
	public void handleItemList(final IItemListHandler handler, long syncTime) throws IOException, ReaderException {
		try {
			String uid = handler.stream(); 
			if (uid.equals(ReaderExtension.STATE_READING_LIST)) {
				for (String[] f : FEEDS) {
					String url = f[0].replace("FEED:", "");
					parseItemList(url, handler, f[0]);
				}
			} else if (uid.startsWith("CAT:")) {
				for (String[] f : FEEDS) {
					if (f[2].equals(uid)) {
						String url = f[0].replace("FEED:", "");
						parseItemList(url, handler, f[0]);						
					}
				}
			} else if (uid.startsWith("FEED:")) {
				String url = handler.stream().replace("FEED:", "");
				parseItemList(url, handler, handler.stream());
			} else if (uid.startsWith("LABEL:")) {
				Log.e("Test", "No url for label");
			}
		}
		catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}

	/*
	 * Get the content of a single feed 
	 * 
	 * API call: https://www.newsblur.com/reader/feeds
	 * Result: 
	 *   feeds/[ID]/feed_address (http://feeds.feedburner.com/codinghorror - rss file)
	 *   feeds/[ID]/feed_title ("Coding Horror")
	 *   feeds/[ID]/feed_link (http://www.codinghorror.com/blog/ - site's link)
	 */
	public void parseItemList(String url, final IItemListHandler handler, final String cat) throws IOException, ReaderException {
		final AQuery aq = new AQuery(this);
		AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>() {
			@Override
			public void callback(String url, JSONObject json, AjaxStatus status) {
				List<IItem> items = new ArrayList<IItem>();
				IItem item = null;
				try {
					if (json != null) {
						JSONArray arr = json.getJSONArray("stories");
						for (int i=0; i<arr.length(); i++) {
							JSONObject story = arr.getJSONObject(i);
							item = new IItem();
							item.subUid = "feed" + url;
							item.title = story.getString("story_title");
							item.link = story.getString("story_permalink");
							item.uid = story.getString("id");
							item.author = story.getString("story_authors");
							item.publishedTime = story.getLong("story_timestamp");
							item.read = (story.getInt("read_status") == 1);
							item.addCategory(cat);
							items.add(item);
						}
						handler.items(items);
					}
					else
					{
						status.getCode();
					}
				}
				catch (Exception e) {
					AQUtility.report(e);
				}
			}
		};
		cb.header("User-Agent", System.getProperty("http.agent"));
		aq.ajax(url, JSONObject.class, cb);
	}	
	
	@Override
	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws IOException, ReaderException {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean markAsRead(String[]  itemUids, String[]  subUIds) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}
	 

	@Override
	public boolean markAsUnread(String[]  itemUids, String[]  subUids, boolean keepUnread) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean markAllAsRead(String s, String t, long syncTime) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean editItemTag(String[]  itemUids, String[]  subUids, String[]  addTags, String[]  removeTags) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean editSubscription(String uid, String title, String url, String[] tag, int action, long syncTime) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
		// TODO Auto-generated method stub
		return false;
	}
	

}
