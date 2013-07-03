package com.noinnion.android.newsplus.extension.cnn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	/*
	 * API call: http://www.newsblur.com/reader/feeds
	 * Result: folders/0/Math/[ID] (ID = 1818)
	 */
	public String[][] CATEGORIES = new String [][] { 
		 {"CAT:Politics", "Politics"},
		 {"CAT:Sport", "Sport"},
         {"LABEL:Favorites", "Favorites"},
     };
		
	/*
	 * API call: http://www.newsblur.com/reader/feeds
	 * Result: 
	 *   feeds/[ID]/feed_address (http://feeds.feedburner.com/codinghorror - rss file)
	 *   feeds/[ID]/feed_title ("Coding Horror")
	 *   feeds/[ID]/feed_link (http://www.codinghorror.com/blog/ - site's link)
	 */
	public String[][] FEEDS = new String [][] { 
		 {"FEED:http://www.newsblur.com/reader/feed/1818:id", "Coding horror", "http://www.codinghorror.com/blog/", ""},
     };
	

	@Override
	public void handleReaderList(ITagListHandler tagHandler, ISubscriptionListHandler subHandler, long syncTime) throws IOException, ReaderException {
		List<ITag> tags = new ArrayList<ITag>();
		List<ISubscription> feeds = new ArrayList<ISubscription>();
		
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
		} catch (RemoteException e) {
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
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
	}

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
							item = new IItem();
							item.subUid = "feed" + url;
							item.title = arr.getJSONObject(i).getString("story_title");
							item.content = arr.getJSONObject(i).getString("story_content");
							item.link = arr.getJSONObject(i).getString("story_permalink");
							item.uid = arr.getJSONObject(i).getString("id");
							items.add(item);
						}
						handler.items(items);
					}
					else
					{
						status.getCode();
					}
				} catch (Exception e) {
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
