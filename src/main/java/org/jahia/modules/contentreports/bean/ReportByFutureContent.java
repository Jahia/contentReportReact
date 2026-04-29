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
import org.jahia.modules.contentreports.service.FutureConditionService;
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
 * The ReportByFutureContent class
 *
 * @author nonico
 */
public class ReportByFutureContent extends QueryReport {
    private static final Logger logger = LoggerFactory.getLogger(ReportByFutureContent.class);
    private ConditionService conditionService;
    private String searchPath;
    private long totalContent;
    private Set<String> seenNodes;

    /**
     * Constructor for ReportByFutureContent
     * @param siteNode JCRSite node
     * @param searchPath path on where to perform the queries
     */
    public ReportByFutureContent(JCRSiteNode siteNode, String searchPath) {
        super(siteNode);
        this.searchPath = searchPath;
        conditionService = new FutureConditionService();
        seenNodes = new HashSet<>();
    }

    @Override public void execute(JCRSessionWrapper session, int offset, int limit)
            throws RepositoryException, JSONException, JahiaException {
        logger.debug("Building jcr sql queryAllNodesWithFutureDates");
        LocalDateTime now = LocalDateTime.now(systemDefault());
        // Format date as yyyy-MM-ddT00:00:00.000+00:00 for JCR SQL-2 CAST AS DATE
        String nowFormatted = DateTimeFormatter.ISO_LOCAL_DATE.format(now) + "T00:00:00.000+00:00";

        String queryConditionVisibilityNodes = "SELECT * FROM [jnt:content] AS parent \n"
                + "INNER JOIN [jnt:conditionalVisibility] as child ON ISCHILDNODE(child,parent) \n";
        String whereInSearchPath = "WHERE ISDESCENDANTNODE(parent,['" + searchPath + "']) \n";
        String innerJoinStartEndDateCondition = "INNER JOIN [jnt:startEndDateCondition] as condition ON ISCHILDNODE(condition,child)\n";
        String innerJoinDayOfWeekCondition = "INNER JOIN [jnt:dayOfWeekCondition] as dow ON ISCHILDNODE(dow,child) \n";
        String innerJoinTimeOfDayCondition = "INNER JOIN [jnt:timeOfDayCondition] as tod ON ISCHILDNODE(tod,child) \n";
        String beforeStartDate = "condition.start > CAST('" + nowFormatted + "' AS DATE)";

        String queryAllNodesWithFutureDates = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + whereInSearchPath
                + "AND " + beforeStartDate;
        logger.debug(queryAllNodesWithFutureDates);

        String queryFutureDateWithDayOfWeek = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinDayOfWeekCondition
                + whereInSearchPath
                + "AND " + beforeStartDate;
        logger.debug(queryFutureDateWithDayOfWeek);

        String queryFutureDateWithTimeOfDay = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinTimeOfDayCondition
                + whereInSearchPath
                + "AND " + beforeStartDate;
        logger.debug(queryFutureDateWithTimeOfDay);

        String queryFutureDatesWithDayOfWeekAndTimeOfDay = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinDayOfWeekCondition
                + innerJoinTimeOfDayCondition
                + whereInSearchPath
                + "AND " + beforeStartDate;

        logger.debug(queryFutureDatesWithDayOfWeekAndTimeOfDay);


        long totalNumOfNodesWithFutureDates = getTotalCount(session, queryAllNodesWithFutureDates);
        long excludedNodeCount = getTotalCount(session, queryFutureDateWithDayOfWeek)
                + getTotalCount(session, queryFutureDateWithTimeOfDay)
                + getTotalCount(session, queryFutureDatesWithDayOfWeekAndTimeOfDay);
        totalContent = totalNumOfNodesWithFutureDates - excludedNodeCount;
        fillReport(session, queryAllNodesWithFutureDates, offset, limit);
    }

    @Override public void addItem(JCRNodeWrapper node) throws RepositoryException {
        Map<String, String> futureConditions = conditionService.getConditions(node);
        if (futureConditions.size() > 0 && !seenNodes.contains(node.getName())) {
            Map<String, String> map = new HashMap<>();
            map.put("name", node.getName());
            map.put("path", node.getParent().getPath());
            map.put("type", String.join("<br/>", node.getNodeTypes()));
            map.put("liveDate", futureConditions.values().stream().iterator().next());
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
            item.put(nodeMap.get("liveDate"));
            jArray.put(item);
        }

        return buildJsonResponse(totalContent, jArray);
    }
}
