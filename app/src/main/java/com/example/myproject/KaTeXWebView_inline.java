package com.example.myproject;

import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class KaTeXWebView_inline {


    public static WebView create(Context context) {
        WebView wv = new WebView(context);
        configure(wv);
        return wv;
    }


    public static void render(WebView wv, String latex) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            wv.getContext().getAssets().open("mathjax_inline_template.html"),
                            StandardCharsets.UTF_8));
            String template = reader.lines().collect(Collectors.joining("\n"));
            reader.close();

            String safeLaTeX = latex.replace("\\", "\\\\")
                                    .replace("\"", "\\\"");

            String finalHtml = template.replace("$$formula$$", safeLaTeX);

            wv.loadDataWithBaseURL(
                    "file:///android_asset/",
                    finalHtml,
                    "text/html",
                    "UTF-8",
                    null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void configure(WebView wv) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setBlockNetworkLoads(true);
    }
}
