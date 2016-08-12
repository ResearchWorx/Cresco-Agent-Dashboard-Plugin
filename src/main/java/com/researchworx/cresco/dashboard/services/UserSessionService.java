package com.researchworx.cresco.dashboard.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserSessionService {
    private static Map<String, String> sessions = new HashMap<>();
    private static Map<String, Long> lastSeens = new HashMap<>();
    private static Map<String, Boolean> rememberMes = new HashMap<>();

    public static String addSession(String user, Boolean rememberMe) {
        String sessionID = java.util.UUID.randomUUID().toString();
        sessions.put(sessionID, user);
        lastSeens.put(sessionID, new Date().getTime());
        rememberMes.put(sessionID, rememberMe);
        return sessionID;
    }

    public static String getUser(String sessionID) {
        return sessions.get(sessionID);
    }

    public static Long getLastSeen(String sessionID) {
        return lastSeens.get(sessionID);
    }

    public static Boolean getRememberMe(String sessionID) {
        return rememberMes.get(sessionID);
    }

    public static void removeSession(String sessionID) {
        sessions.remove(sessionID);
    }

    public static void seen(String sessionID) {
        lastSeens.put(sessionID, new Date().getTime());
    }
}
