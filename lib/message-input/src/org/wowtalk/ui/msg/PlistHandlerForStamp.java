package org.wowtalk.ui.msg;

import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PlistHandlerForStamp extends DefaultHandler {

    private boolean mIsKeyElement = false;
    private boolean mIsValueElement = false;
    private boolean mIsFirstDict = true;

    private HashMap<String, Object> mRoot;
    private HashMap<String, Object> mCurrentMap;

    private String mKey;
    private Stack<String> mStackKeys = new Stack<String>();
    private Stack<HashMap<String, Object>> mStackValues = new Stack<HashMap<String,Object>>();

    public PlistHandlerForStamp() {
    }

    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        mRoot = new HashMap<String, Object>();
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if ("dict".equals(qName)) {
            // <key>version</key>
            if (mIsFirstDict) {
                mCurrentMap = mRoot;
                mStackValues.push(mCurrentMap);
                mIsFirstDict = false;
            } else {
                HashMap<String, Object> map = new HashMap<String, Object>();
                mCurrentMap.put(mKey, map);
                mCurrentMap = map;
                mStackValues.push(mCurrentMap);
            }
        } else if ("key".equals(qName)) {
            mIsKeyElement = true;
        } else if ("string".equals(qName)) {
            mIsValueElement = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        super.endElement(uri, localName, qName);
        if ("key".equals(qName)) {
            mIsKeyElement = false;
        } else if ("string".equals(qName)) {
            mIsValueElement = false;
        } else if ("dict".equals(qName)) {
            if (!mIsFirstDict) {
                mStackValues.pop();
                if (!mStackValues.isEmpty()) {
                    mCurrentMap = mStackValues.peek();
                }
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        super.characters(ch, start, length);
        if (length > 0) {
            String nodeValue = new String(ch, start, length);
            if (mIsKeyElement) {
                mKey = nodeValue;
                mStackKeys.push(mKey);
            }
            if (mIsValueElement) {
                mCurrentMap.put(mStackKeys.pop(), nodeValue);
            }
        }
    }

    public HashMap<String, Object> getMaResult() {
        return mRoot;
    }
}
