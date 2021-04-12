package com.holy.mytrace.helpers;


import android.util.Xml;

import com.holy.mytrace.models.Hospital;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HospitalXmlParser {

    private static final String ns = null;

    // 주어진 XML 파싱하기
    public List<Hospital> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readResponse(parser);
        } finally {
            in.close();
        }
    }

    // response 태그 읽기
    private List<Hospital> readResponse(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "response");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // body 태그 찾기
            if (name.equals("body")) {
                return readBody(parser);
            } else {
                skip(parser);
            }
        }
        return null;
    }

    // body 태그 읽기
    private List<Hospital> readBody(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "body");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // items 태그 찾기
            if (name.equals("items")) {
                return readItems(parser);
            } else {
                skip(parser);
            }
        }
        return null;
    }

    // items 태그 읽기
    private List<Hospital> readItems(XmlPullParser parser) throws XmlPullParserException, IOException {

        List<Hospital> articleList = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, ns, "items");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // item 태그 찾기
            if (name.equals("item")) {
                articleList.add(readItem(parser));
            } else {
                skip(parser);
            }
        }
        return articleList;
    }

    // item 태그 읽기
    private Hospital readItem(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "item");
        String name = null;
        String address = null;
        String tel = null;
        double longitude = 0;
        double latitude = 0;
        double distance = 0;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tag = parser.getName();
            switch (tag) {
                case "yadmNm":
                    name = readName(parser);
                    break;
                case "addr":
                    address = readAddress(parser);
                    break;
                case "telno":
                    tel = readTel(parser);
                    break;
                case "XPos":
                    longitude = readLongitude(parser);
                    break;
                case "YPos":
                    latitude = readLatitude(parser);
                    break;
                case "distance":
                    distance = readDistance(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return new Hospital(name, address, tel, latitude, longitude, distance);
    }

    // title 태그의 내용 읽기
    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "yadmNm");
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "yadmNm");
        return name;
    }

    // addr 태그의 내용 읽기
    private String readAddress(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "addr");
        String address = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "addr");
        return address;
    }

    // telno 태그의 내용 읽기
    private String readTel(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "telno");
        String tel = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "telno");
        return tel;
    }

    // XPos 태그의 내용 읽기
    private double readLongitude(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "XPos");
        String strLongitude = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "XPos");
        return Double.parseDouble(strLongitude);
    }

    // YPos 태그의 내용 읽기
    private double readLatitude(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "YPos");
        String strLatitude = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "YPos");
        return Double.parseDouble(strLatitude);
    }

    // distance 태그의 내용 읽기
    private double readDistance(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, ns, "distance");
        String strDistance = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "distance");
        return Double.parseDouble(strDistance);
    }

    // 태그 안의 텍스트 읽기
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {

        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // 태그 건너뛰기
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {

        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}




