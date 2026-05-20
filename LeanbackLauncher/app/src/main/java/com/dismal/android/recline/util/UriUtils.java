package com.dismal.android.recline.util;

import android.content.Intent;
import android.net.Uri;

/* JADX INFO: loaded from: classes.dex */
public final class UriUtils {
    private UriUtils() {
    }

    public static boolean isAndroidResourceUri(Uri uri) {
        return "android.resource".equals(uri.getScheme());
    }

    public static boolean isContentUri(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            return true;
        }
        return "file".equals(uri.getScheme());
    }

    public static boolean isShortcutIconResourceUri(Uri uri) {
        return "shortcut.icon.resource".equals(uri.getScheme());
    }

    public static Intent.ShortcutIconResource getIconResource(Uri uri) {
        if (isAndroidResourceUri(uri)) {
            Intent.ShortcutIconResource iconResource = new Intent.ShortcutIconResource();
            iconResource.packageName = uri.getAuthority();
            iconResource.resourceName = uri.toString().substring("android.resource".length() + "://".length()).replaceFirst("/", ":");
            return iconResource;
        }
        if (isShortcutIconResourceUri(uri)) {
            Intent.ShortcutIconResource iconResource2 = new Intent.ShortcutIconResource();
            iconResource2.packageName = uri.getAuthority();
            iconResource2.resourceName = uri.toString().substring("shortcut.icon.resource".length() + "://".length() + iconResource2.packageName.length() + "/".length()).replaceFirst("/", ":");
            return iconResource2;
        }
        throw new IllegalArgumentException("Invalid resource URI. " + uri);
    }

    public static boolean isWebUri(Uri resourceUri) {
        String lowerCase = resourceUri.getScheme() == null ? null : resourceUri.getScheme().toLowerCase();
        if ("http".equals(lowerCase)) {
            return true;
        }
        return "https".equals(lowerCase);
    }
}
