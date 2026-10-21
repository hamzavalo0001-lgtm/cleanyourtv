package com.gathin.tvhome;

/** Weather for Batman (centre) from open-meteo.com: free, no key. Asked at most every 30 minutes on its own
 *  thread; on a failure the text keeps its last value and the next try is in 5 minutes. */
final class Weather {
    private static final long EVERY_MS = 30L * 60 * 1000;
    private static final long RETRY_MS = 5L * 60 * 1000;
    private static final String URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=37.8812&longitude=41.1351&current=temperature_2m,weather_code&timezone=auto";

    private static volatile String text;     // e.g. "Batman 18\u00B0C A\u00E7\u0131k"; null until the first success
    private static volatile long dueAt;      // earliest time of the next request
    private static volatile boolean busy;

    private Weather() {
    }

    static String text() {
        return text;
    }

    static void refreshIfDue() {
        long now = System.currentTimeMillis();
        if (busy || now < dueAt) return;
        busy = true;
        dueAt = now + EVERY_MS;
        new Thread(() -> {
            java.net.HttpURLConnection c = null;
            try {
                c = (java.net.HttpURLConnection) new java.net.URL(URL).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                try (java.io.InputStream in = c.getInputStream()) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[1024];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    org.json.JSONObject cur = new org.json.JSONObject(out.toString("UTF-8")).getJSONObject("current");
                    String word = word(cur.getInt("weather_code"));
                    text = "Batman " + Math.round(cur.getDouble("temperature_2m")) + "\u00B0C"
                            + (word.isEmpty() ? "" : " " + word);
                }
            } catch (Exception e) {
                dueAt = System.currentTimeMillis() + RETRY_MS;
            } finally {
                if (c != null) c.disconnect();
                busy = false;
            }
        }, "weather").start();
    }

    private static String word(int code) {
        if (code == 0) return "A\u00E7\u0131k";
        if (code <= 3) return "Bulutlu";
        if (code == 45 || code == 48) return "Sisli";
        if (code >= 51 && code <= 67) return "Ya\u011Fmurlu";
        if (code >= 71 && code <= 77) return "Kar";
        if (code >= 80 && code <= 82) return "Sa\u011Fanak";
        if (code >= 85 && code <= 86) return "Kar";
        if (code >= 95) return "F\u0131rt\u0131na";
        return "";
    }
}
