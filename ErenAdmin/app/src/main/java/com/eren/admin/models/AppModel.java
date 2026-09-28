package com.eren.admin.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppModel {
    public String id;
    public String name;
    public String shortDetail;
    public String iconUrl;
    public String connectKey;
    public String databaseURL;
    public boolean dialogEnabled = true;
    public long createdAt;
    public DialogConfig dialogConfig = new DialogConfig();

    public static class DialogConfig {
        public String imageUrl = "";
        public String brandName = "MODMASE";
        public String brandHighlight = "MASE";
        public List<String> features = new ArrayList<>();
        public String updateUrl = "";
        public String accentColor = "#70A0DF";
        public String bgColor = "#FFD0C8";
        public String cardBg = "#FFFAFD";

        public DialogConfig() {
            features.add("New Features Available");
            features.add("Previous Bug Fixed");
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("imageUrl", imageUrl);
            m.put("brandName", brandName);
            m.put("brandHighlight", brandHighlight);
            m.put("features", features);
            m.put("updateUrl", updateUrl);
            m.put("accentColor", accentColor);
            m.put("bgColor", bgColor);
            m.put("cardBg", cardBg);
            return m;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("shortDetail", shortDetail);
        m.put("iconUrl", iconUrl);
        m.put("connectKey", connectKey);
        m.put("databaseURL", databaseURL);
        m.put("dialogEnabled", dialogEnabled);
        m.put("createdAt", createdAt);
        m.put("dialogConfig", dialogConfig.toMap());
        return m;
    }

    public static AppModel fromSnapshot(String id, Map<String, Object> data) {
        AppModel a = new AppModel();
        a.id = id;
        a.name = str(data.get("name"));
        a.shortDetail = str(data.get("shortDetail"));
        a.iconUrl = str(data.get("iconUrl"));
        a.connectKey = str(data.get("connectKey"));
        a.databaseURL = str(data.get("databaseURL"));
        Object en = data.get("dialogEnabled");
        a.dialogEnabled = en == null || Boolean.TRUE.equals(en);
        Object ca = data.get("createdAt");
        if (ca instanceof Long) a.createdAt = (Long) ca;
        else if (ca instanceof Integer) a.createdAt = ((Integer) ca).longValue();

        Object cfgObj = data.get("dialogConfig");
        if (cfgObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cfg = (Map<String, Object>) cfgObj;
            a.dialogConfig.imageUrl = str(cfg.get("imageUrl"));
            a.dialogConfig.brandName = str(cfg.get("brandName"), "MODMASE");
            a.dialogConfig.brandHighlight = str(cfg.get("brandHighlight"), "MASE");
            a.dialogConfig.updateUrl = str(cfg.get("updateUrl"));
            a.dialogConfig.accentColor = str(cfg.get("accentColor"), "#70A0DF");
            a.dialogConfig.bgColor = str(cfg.get("bgColor"), "#FFD0C8");
            a.dialogConfig.cardBg = str(cfg.get("cardBg"), "#FFFAFD");
            Object feats = cfg.get("features");
            a.dialogConfig.features = new ArrayList<>();
            if (feats instanceof List) {
                for (Object o : (List<?>) feats) {
                    if (o != null) a.dialogConfig.features.add(o.toString());
                }
            }
            if (a.dialogConfig.features.isEmpty()) {
                a.dialogConfig.features.add("New Features Available");
                a.dialogConfig.features.add("Previous Bug Fixed");
            }
        }
        return a;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String str(Object o, String def) {
        return o == null || o.toString().isEmpty() ? def : o.toString();
    }
}
