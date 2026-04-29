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
import org.jahia.modules.contentreports.service.ExpiredConditionService;
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
 * The ReportByExpiredContent class
 *
 * @author nonico
 */
public class ReportByExpiredContent extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportByExpiredContent.class);
    private ConditionService conditionService;
    private long totalContent;
    private String searchPath;
    private Set<String> seenNodes;


    /**
     * Constructor for ReportByExpiredContent
     * @param siteNode JCRSite node
     * @param searchPath path on where to perform the queries
     */
    public ReportByExpiredContent(JCRSiteNode siteNode, String searchPath) {
        super(siteNode);
        this.searchPath = searchPath;
        this.conditionService = new ExpiredConditionService();
        seenNodes = new HashSet<>();
    }

    @Override public void execute(JCRSessionWrapper session, int offset, int limit)
            throws RepositoryException, JSONException, JahiaException {
        logger.debug("Building jcr sql queryNodesWithExpiredDates");
        LocalDateTime now = LocalDateTime.now(systemDefault());
        // Format date as yyyy-MM-ddT00:00:00.000+00:00 for JCR SQL-2 CAST AS DATE
        String nowFormatted = DateTimeFormatter.ISO_LOCAL_DATE.format(now) + "T00:00:00.000+00:00";

        String queryConditionVisibilityNodes = "SELECT * FROM [jnt:content] AS parent \n"
                + "INNER JOIN [jnt:conditionalVisibility] as child ON ISCHILDNODE(child,parent) \n";
        String whereInSearchPath = "WHERE ISDESCENDANTNODE(parent,['" + searchPath + "']) \n";
        String innerJoinStartEndDateCondition = "INNER JOIN [jnt:startEndDateCondition] as condition ON ISCHILDNODE(condition,child)\n";
        String innerJoinDayOfWeekCondition = "INNER JOIN [jnt:dayOfWeekCondition] as dow ON ISCHILDNODE(dow,child) \n";
        String innerJoinTimeOfDayCondition = "INNER JOIN [jnt:timeOfDayCondition] as tod ON ISCHILDNODE(tod,child) \n";
        String afterEndDate = "condition.end < CAST('"+ nowFormatted +"' AS DATE) ORDER BY parent.Name ASC";

        String queryNodesWithExpiredDates = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + whereInSearchPath
                + "AND " + afterEndDate;
        logger.debug(queryNodesWithExpiredDates);

        String queryStartEndNodesWithDayOfWeek = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinDayOfWeekCondition
                + whereInSearchPath
                + "AND " + afterEndDate;
        logger.debug(queryStartEndNodesWithDayOfWeek);

        String queryStartEndNodesWithTimeOfDay = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinTimeOfDayCondition
                + whereInSearchPath
                + "AND " + afterEndDate;
        logger.debug(queryStartEndNodesWithTimeOfDay);

        String queryStartEndNodesWithTimeOfDayAndDayOfWeek = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinTimeOfDayCondition
                + innerJoinDayOfWeekCondition
                + whereInSearchPath
                + "AND " + afterEndDate;
        logger.debug(queryStartEndNodesWithTimeOfDayAndDayOfWeek);

        long totalNumOfNodesExpiredDates = getTotalCount(session, queryNodesWithExpiredDates);
        long excludedNodes = getTotalCount(session, queryStartEndNodesWithDayOfWeek)
                + getTotalCount(session, queryStartEndNodesWithTimeOfDay)
                + getTotalCount(session, queryStartEndNodesWithTimeOfDayAndDayOfWeek);
        totalContent = totalNumOfNodesExpiredDates - excludedNodes;
        fillReport(session, queryNodesWithExpiredDates, offset, limit);
    }

    @Override public void addItem(JCRNodeWrapper node) throws RepositoryException {

        Map<String, String> expiredConditions = conditionService.getConditions(node);
        if (expiredConditions.size() == 1 && !seenNodes.contains(node.getName())) {
            Map<String, String> map = new HashMap<>();
            map.put("name", node.getName());
            map.put("path", node.getParent().getPath());
            map.put("type", String.join("<br/>", node.getNodeTypes()));
            map.put("expiresOn", expiredConditions.values().iterator().next());
            this.dataList.add(map);
            this.seenNodes.add(node.getName());
        }
    }
    @Override public JSONObject getJson() throws JSONException, RepositoryException {

        JSONArray jArray = new JSONArray();

        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("name"));
            item.put(nodeMap.get("path"));
            item.put(nodeMap.get("type"));
            item.put(nodeMap.get("expiresOn"));
            jArray.put(item);
        }

        return buildJsonResponse(totalContent, jArray);
    }
}
