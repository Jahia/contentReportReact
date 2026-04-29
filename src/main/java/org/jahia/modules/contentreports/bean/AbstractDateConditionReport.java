/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.contentreports.bean;

import org.jahia.exceptions.JahiaException;
import org.jahia.modules.contentreports.service.ConditionService;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.time.ZoneId.systemDefault;

/**
 * Abstract base for reports that query content by a date visibility condition.
 * Subclasses provide the specific date condition string, match predicate, and output key.
 */
public abstract class AbstractDateConditionReport extends QueryReport {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDateConditionReport.class);

    protected ConditionService conditionService;
    protected long totalContent;
    protected final String searchPath;
    protected Set<String> seenNodes;

    protected AbstractDateConditionReport(JCRSiteNode siteNode, String searchPath) {
        super(siteNode);
        this.searchPath = searchPath;
        this.seenNodes = new HashSet<>();
    }

    /**
     * Returns the SQL-2 WHERE condition fragment (after "AND ") for the date filter,
     * given the current timestamp formatted as {@code yyyy-MM-ddT00:00:00.000+00:00}.
     */
    protected abstract String buildDateCondition(String nowFormatted);

    /**
     * Returns {@code true} when the node's conditions map passes this report's filter.
     */
    protected abstract boolean matchesCondition(Map<String, String> conditions);

    /**
     * Returns the key used to store the date value in the result map and JSON output.
     */
    protected abstract String getDateKey();

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit)
            throws RepositoryException, JSONException, JahiaException {

        LocalDateTime now = LocalDateTime.now(systemDefault());
        String nowFormatted = DateTimeFormatter.ISO_LOCAL_DATE.format(now) + "T00:00:00.000+00:00";
        String dateCondition = buildDateCondition(nowFormatted);

        String queryBase = "SELECT * FROM [jnt:content] AS parent \n"
                + "INNER JOIN [jnt:conditionalVisibility] as child ON ISCHILDNODE(child,parent) \n";
        String whereClause = "WHERE ISDESCENDANTNODE(parent,['" + searchPath + "']) \n";
        String joinStartEnd = "INNER JOIN [jnt:startEndDateCondition] as condition ON ISCHILDNODE(condition,child)\n";
        String joinDayOfWeek = "INNER JOIN [jnt:dayOfWeekCondition] as dow ON ISCHILDNODE(dow,child) \n";
        String joinTimeOfDay = "INNER JOIN [jnt:timeOfDayCondition] as tod ON ISCHILDNODE(tod,child) \n";
        String andCondition = "AND " + dateCondition;

        String mainQuery = queryBase + joinStartEnd + whereClause + andCondition;
        logger.debug(mainQuery);
        String queryWithDayOfWeek = queryBase + joinStartEnd + joinDayOfWeek + whereClause + andCondition;
        logger.debug(queryWithDayOfWeek);
        String queryWithTimeOfDay = queryBase + joinStartEnd + joinTimeOfDay + whereClause + andCondition;
        logger.debug(queryWithTimeOfDay);
        String queryWithBoth = queryBase + joinStartEnd + joinTimeOfDay + joinDayOfWeek + whereClause + andCondition;
        logger.debug(queryWithBoth);

        long total = getTotalCount(session, mainQuery);
        long excluded = getTotalCount(session, queryWithDayOfWeek)
                + getTotalCount(session, queryWithTimeOfDay)
                + getTotalCount(session, queryWithBoth);
        totalContent = total - excluded;
        fillReport(session, mainQuery, offset, limit);
    }

    @Override
    public void addItem(JCRNodeWrapper node) throws RepositoryException {
        Map<String, String> conditions = conditionService.getConditions(node);
        if (matchesCondition(conditions) && !seenNodes.contains(node.getName())) {
            Map<String, String> map = new HashMap<>();
            map.put("name", node.getName());
            map.put("path", node.getParent().getPath());
            map.put("type", String.join("<br/>", node.getNodeTypes()));
            map.put(getDateKey(), conditions.values().iterator().next());
            this.dataList.add(map);
            this.seenNodes.add(node.getName());
        }
    }

    @Override
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONArray jArray = new JSONArray();
        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("name"));
            item.put(nodeMap.get("path"));
            item.put(nodeMap.get("type"));
            item.put(nodeMap.get(getDateKey()));
            jArray.put(item);
        }
        return buildJsonResponse(totalContent, jArray);
    }
}
