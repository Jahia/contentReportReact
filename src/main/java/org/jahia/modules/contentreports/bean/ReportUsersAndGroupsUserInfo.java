package org.jahia.modules.contentreports.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportUsersAndGroupsUserInfo {

    private final String site;
    private final String name;
    private final List<String> groups = new ArrayList<>();
    private final Map<String, String> properties = new HashMap<>();

    public ReportUsersAndGroupsUserInfo(String site, String name) {
        this.site = site;
        this.name = name;
    }

    public String getSite() {
        return site;
    }

    public String getName() {
        return name;
    }

    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public void addGroup(String group) {
        groups.add(group);
    }

    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    public boolean containsPropertyKey(String key) {
        return properties.containsKey(key);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }
}
