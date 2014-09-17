package co.onemeter.oneapp.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StringToJason {
    private JSONObject jsonObj;

    public StringToJason(String jsonString) {
        if(null == jsonString) {
            jsonObj=null;
            return;
        }
        try {
            jsonObj = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public JSONObject getInnerObject(String objName) {
        JSONObject innerObj=null;

        try {
            innerObj=jsonObj.optJSONObject(objName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return innerObj;
    }
    
    public JSONArray getInnerArray(String aName) {
        JSONArray innerArray=null;

        
        try {
            innerArray=jsonObj.optJSONArray(aName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return innerArray;
    }
    
    public String getValue(String innerObject,String key) {
        String value=null;

        try {
            JSONObject object=jsonObj.optJSONObject(innerObject);
            if(null != object) {
                value=object.optString(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return value;
    }
    
    public String getValue(String key) {
        String value=null;

        try {
            value=jsonObj.optString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public String getError() {
        return getValue("err_no");
    }
}