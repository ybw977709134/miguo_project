package org.wowtalk.ui.msg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;

/**
 * Format/parse stamp message, load stamp configurations, etc.
 */
public class Stamp {
	
	public static final String HOME = "/wowtalk/.cache/moji/";
	
	/**
	 * stamp animation.
	 */
	public static final int TYPEID_ANIME = 1;
	/**
	 * static emotion.
	 */
	public static final int TYPEID_IMAGE = 2;
	/**
	 * kaomoji.
	 */
	public static final int TYPEID_KAOMOJI = 3;
	
	// parts of file name
	public static final String TYPENAME_ANIME = "anime"; 
	public static final String TYPENAME_IMAGE = "image";
	// for formatting JSON message content:
	public static final String JSON_STAMPTYPE_ANIME = "stamp_anime"; 
	public static final String JSON_STAMPTYPE_IMAGE = "stamp_image";
	public static final String COLOREDPACKIMAGES = "coloredpackimages";
	public static final String PACKIMAGES = "packimages";
	public static final String THUMBS = "thumbs";

    public static final int KAOMOJI_TAB_COUNT = 6;
	
	public static HashMap<String, Object> configs;
	
	private String filePath;
	
	/**
	 * equals folder name
	 */
	private String packid;

	/**
	 * equals folder name
	 */
	private String stampid;
	
	/**
	 * TYPE_* constants
	 */
	private int stampType;
	
	private int stampAnimeWidth;
	
	private int stampAnimeHeight;
	
	private int stampImageWidth;
	
	private int stampImageHeight;
	
	private boolean stampAutoPlay;
	
	private Context mContext;
	
	public Stamp() {
	}
	
	/**
	 * 
	 * @param type TYPE_ANIME | TYPE_IMAGE
	 * @param packID
	 * @param stampID
	 */
	public Stamp(Context context, int type, String packID, String stampID) {
		mContext = context;
		this.packid = packID;
		this.stampid = stampID;
		stampType = type;
		try {
			parseSize();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		setFilePath();
	}

	private void setFilePath() {
		String type = null;
		if (stampType == TYPEID_ANIME) {
			type = "anime";
		} else {
			type = "images";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("stamp").append(File.separator);
		sb.append(type).append(File.separator);
		sb.append(packid).append(File.separator);
		sb.append(stampid).append(".png");
		filePath = sb.toString();
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getPackID() {
		return packid;
	}

	public void setPackID(String packID) {
		this.packid = packID;
	}

	public String getStampID() {
		return stampid;
	}

	public void setStampID(String stampID) {
		this.stampid = stampID;
	}

	public int getStampType() {
		return stampType;
	}

	public void setStampType(int stampType) {
		this.stampType = stampType;
	}

	public int getStampAnimeWidth() {
		return stampAnimeWidth;
	}

	public void setStampAnimeWidth(int stampAnimeWidth) {
		this.stampAnimeWidth = stampAnimeWidth;
	}

	public int getStampAnimeHeight() {
		return stampAnimeHeight;
	}

	public void setStampAnimeHeight(int stampAnimeHeight) {
		this.stampAnimeHeight = stampAnimeHeight;
	}

	public int getStampImageWidth() {
		return stampImageWidth;
	}

	public void setStampImageWidth(int stampImageWidth) {
		this.stampImageWidth = stampImageWidth;
	}

	public int getStampImageHeight() {
		return stampImageHeight;
	}

	public void setStampImageHeight(int stampImageHeight) {
		this.stampImageHeight = stampImageHeight;
	}

	public boolean isStampAutoPlay() {
		return stampAutoPlay;
	}

	public void setStampAutoPlay(boolean stampAutoPlay) {
		this.stampAutoPlay = stampAutoPlay;
	}
	
	private void parseSize() throws JSONException {
		StringBuffer sb = new StringBuffer();
		try {
			InputStream fis = mContext.getAssets().open(
					String.format("wowtalk/stamp/%s/%s/packageinfo.json", 
							(stampType == TYPEID_ANIME ? TYPENAME_ANIME : TYPENAME_IMAGE),
							packid),
					AssetManager.ACCESS_BUFFER);
			int c;
			while ((c = fis.read()) != -1) {
				sb.append((char) c);
			}
			fis.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String string = sb.toString();
		JSONObject object = new JSONObject(string);
		if (stampType == TYPEID_ANIME) {
			stampAnimeWidth = Integer.valueOf(object.getString("width").substring(2, object.getString("width").length() - 2));
			stampAnimeHeight = Integer.valueOf(object.getString("height").substring(2, object.getString("height").length() - 2));
		} else {
			stampImageWidth = Integer.valueOf(object.getString("width").substring(2, object.getString("width").length() - 2));
			stampImageHeight = Integer.valueOf(object.getString("height").substring(2, object.getString("height").length() - 2));
			
		}
		
	}
	
	public String getMessageContent() {
		if (stampType == TYPEID_ANIME) {
			return getAnimeContent();
		} else {
			return getImageContent();
		}
	}
	
	private String getAnimeContent() {
		JSONObject json = new JSONObject();
		try {
		    json.put("stamptype", stampType == TYPEID_IMAGE ? JSON_STAMPTYPE_IMAGE : JSON_STAMPTYPE_ANIME);
			json.put("stamp_autoplay", "do_autoplay");
			json.put("stamp_anime_w", stampAnimeWidth);
			json.put("stamp_anime_h", stampAnimeHeight);
			json.put("packid", String.valueOf(packid));
			json.put("stampid", String.valueOf(stampid));
			json.put("filepath", filePath);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
	
	private String getImageContent() {
		JSONObject json = new JSONObject();
		try {
		    json.put("stamptype", stampType == TYPEID_IMAGE ? JSON_STAMPTYPE_IMAGE : JSON_STAMPTYPE_ANIME);
			json.put("stamp_image_w", stampImageWidth);
			json.put("stamp_image_h", stampImageHeight);
			json.put("packid", packid);
			json.put("stampid", stampid);
			json.put("filepath", filePath);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
	
	public static Stamp parseJSON(String str) {
		try {
			Stamp s = new Stamp();
			JSONObject json = new JSONObject(str);
			String type = json.getString("stamptype");
			if (type.equals(Stamp.JSON_STAMPTYPE_ANIME)) {
				s.stampAnimeWidth = json.getInt("stamp_anime_w");
				s.stampAnimeHeight = json.getInt("stamp_anime_h");
			} else {
				s.stampImageWidth = json.getInt("stamp_image_w");
				s.stampImageHeight = json.getInt("stamp_image_h");
			}
			s.packid = json.getString("packid");
			s.stampid = json.getString("stampid");
			return s;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static HashMap<String, Object> loadConfig(Context context) {
		if(configs != null) return configs;
		
		try {
			InputStream is = context.getAssets().open(
					"wowtalk/stamp/stampconfig.plist",
					AssetManager.ACCESS_BUFFER);

			
			SAXParserFactory factorys = SAXParserFactory.newInstance();
			SAXParser parser = factorys.newSAXParser();
			PlistHandlerForStamp handler = new PlistHandlerForStamp();
			parser.parse(is, handler);
			
			is.close();
			
			return configs = (HashMap<String, Object>)handler.getMaResult();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @param context
	 * @param stampType Stamp.TYPE_* constants
	 * @param packIndex 0-based
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getPackId(Context context, int stampType, int packIndex) {
		try {
			HashMap<String, Object> configs = loadConfig(context);
			if(configs == null) return null;
			
			HashMap<String, HashMap<String, String>> m = 
					(HashMap<String, HashMap<String, String>> )configs.get(
					stampType == TYPEID_ANIME ? TYPENAME_ANIME : TYPENAME_IMAGE);
			return m.get(Integer.toString(packIndex)).get("packid");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
