package com.unimatrix.service;
import com.unimatrix.model.*; import java.util.Locale;
public final class TrackRules { private TrackRules(){} public static boolean compatible(Course c,BlockSection b){if(c==null||b==null)return true;String x=norm(c.getTrack()),y=norm(b.getTrack());return x.equals("general")||y.equals("general")||x.equals(y);}private static String norm(String s){return s==null||s.isBlank()?"general":s.trim().toLowerCase(Locale.ROOT);}}
