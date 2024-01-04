package com.threethan.browser.lib;

public class StringLib {
    public static boolean isValidUrl(String url) {
        if (url.startsWith("about:")) return true;
        return (!(!url.contains("://") || !url.contains(".")));
    }
    public static final String GOOGLE_SEARCH_PRE = "https://www.google.com/search?q=";
    public static String googleSearchForUrl(String string) {
        return GOOGLE_SEARCH_PRE+string;
    }
    public static String toValidUrl(String url) {
        if (StringLib.isValidUrl(url)) return url;
        else if (StringLib.isValidUrl("https://" + url)) return "https://" + url;
        else return StringLib.googleSearchForUrl(url);
    }
    public static class ParititionedUrl {
        public final String prefix;
        public final String middle;
        public final String suffix;

        public ParititionedUrl(String url) {
            url = url.replace("https://","");
            String[] split = url.split("\\.");

            if (split.length <= 1) {
                this.prefix = "";
                this.middle = url;
                this.suffix = "";
            } else if (split.length == 2) {
                this.prefix = "";
                this.middle = split[0];
                this.suffix = url.replace(split[0], "");
            } else {
                this.prefix = split[0] + ".";
                this.middle = split[1];
                this.suffix = url.replace(split[0]+"."+split[1], "");
            }
        }
    }
}
