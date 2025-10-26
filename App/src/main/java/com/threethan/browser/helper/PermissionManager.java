package com.threethan.browser.helper;

import android.Manifest;
import android.app.Activity;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {
    public static final String[] KNOWN_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static DataStoreEditor dataStoreEditor;
    private static final String KEY_ORIGINS = "KEY_ORIGINS";
    private static final Set<String> managedOrigins = ConcurrentHashMap.newKeySet();
    public PermissionManager(Activity activity) {
        dataStoreEditor = new DataStoreEditor(activity, "permissions");
        Set<String> storedOrigins = dataStoreEditor.getStringSet(KEY_ORIGINS, new HashSet<>());
        managedOrigins.addAll(storedOrigins);
    }

    public boolean getPermission(String origin, String permissionType) {
        String key = origin + "_" + permissionType;
        return dataStoreEditor.getBoolean(key, false);
    }

    public void setPermission(String origin, String permissionType, boolean isGranted) {
        String key = origin + "_" + permissionType;
        dataStoreEditor.putBoolean(key, isGranted);
        managedOrigins.add(origin);
        dataStoreEditor.putStringSet(KEY_ORIGINS, managedOrigins);
        Log.i("PermissionManager", "Permission " + permissionType + " for " + origin + " set to " + isGranted);
    }
    public boolean hasPermissionsForOrigin(String origin) {
        for (String permission : KNOWN_PERMISSIONS) {
            if (getPermission(origin, permission)) {
                return true;
            }
        }
        return false;
    }

    public String[] getPermissionsForOrigin(String origin) {
        Set<String> grantedPermissions = new HashSet<>();
        for (String permission : KNOWN_PERMISSIONS) {
            if (getPermission(origin, permission)) {
                grantedPermissions.add(permission);
            }
        }
        return grantedPermissions.toArray(new String[0]);
    }
}
