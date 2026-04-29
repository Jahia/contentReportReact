/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.contentreports.bean;

import org.jahia.exceptions.JahiaException;
import org.jahia.modules.contentreports.service.ConditionService;
import org.jahia.modules.contentreports.service.LiveConditionService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.time.ZoneId.of;
import static java.time.ZoneId.systemDefault;
import static org.jahia.modules.contentreports.service.LiveConditionService.CURRENT_STATUS;
import static org.jahia.modules.contentreports.service.LiveConditionService.IS_CONDITION_MATCHED;

/**
 * The ReportLiveContents Class.
 *
 * @author nonico
 */
public class ReportLiveContents extends QueryReport {
    private static final Logger logger = LoggerFactory.getLogger(ReportLiveContents.class);
    private final ConditionService conditionService;
    private String searchPath;
    private long totalContent;
    private static final String IS_CONDITION_MATCHED_PROP = "isConditionMatched";
    private static final String LIST_OF_CONDITIONS_PROP = "listOfConditions";
    private static final String CURRENT_STATUS_PROP = "currentStatus";

    /**
     * Constructor for ReportLiveContents
     * @param siteNode JCRSite node
     * @param searchPath path on where to perform the queries
     */
    public ReportLiveContents(JCRSiteNode siteNode, String searchPath) {
        super(siteNode);
        this.searchPath = searchPath;
        this.conditionService = new LiveConditionService();
    }

    @Override public void execute(JCRSessionWrapper session, int offset, int limit)
            throws RepositoryException, JSONException, JahiaException {
        logger.debug("Building jcr sql query");
        LocalDateTime now = LocalDateTime.now(systemDefault());
        // Format date as yyyy-MM-ddT00:00:00.000+00:00 for JCR SQL-2 CAST AS DATE
        String nowFormatted = DateTimeFormatter.ISO_LOCAL_DATE.format(now) + "T00:00:00.000+00:00";
        
        String queryConditionVisibilityNodes = "SELECT * FROM [jnt:content] AS parent \n"
                + "INNER JOIN [jnt:conditionalVisibility] as child ON ISCHILDNODE(child,parent) \n";
        String whereInSearchPath = "WHERE ISDESCENDANTNODE(parent,['" + searchPath + "']) \n";
        String innerJoinStartEndDateCondition = "INNER JOIN [jnt:startEndDateCondition] as condition ON ISCHILDNODE(condition,child)\n";
        String innerJoinDayOfWeekCondition = "INNER JOIN [jnt:dayOfWeekCondition] as dow ON ISCHILDNODE(dow,child) \n";
        String innerJoinTimeOfDayCondition = "INNER JOIN [jnt:timeOfDayCondition] as tod ON ISCHILDNODE(tod,child) \n";
        String beforeStartDate = "condition.start > CAST('"+ nowFormatted +"' AS DATE)";
        String afterEndDate = "condition.end < CAST('"+ nowFormatted +"' AS DATE)";
        String andWithInvalidDates = String.format("AND (%s OR %s)", beforeStartDate, afterEndDate);

        String query = queryConditionVisibilityNodes + whereInSearchPath;
        logger.debug(query);

        String queryInvalidStartEndDate = queryConditionVisibilityNodes
                        + innerJoinStartEndDateCondition
                        + whereInSearchPath
                        + andWithInvalidDates;
        logger.debug(queryInvalidStartEndDate);
        String queryInvalidStartEndWithDayOfWeek = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinDayOfWeekCondition
                + whereInSearchPath
                + andWithInvalidDates;
        logger.debug(queryInvalidStartEndWithDayOfWeek);


        String queryInvalidStartEndWithTimeOfDay = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinTimeOfDayCondition
                + whereInSearchPath
                + andWithInvalidDates;
        logger.debug(queryInvalidStartEndWithTimeOfDay);

        String queryInvalidStartEndWithDayOfWeekAndTimeOfDay = queryConditionVisibilityNodes
                + innerJoinStartEndDateCondition
                + innerJoinDayOfWeekCondition
                + innerJoinTimeOfDayCondition
                + whereInSearchPath
                + andWithInvalidDates;
        logger.debug(queryInvalidStartEndWithDayOfWeekAndTimeOfDay);
        long totalNumOfNodesWithConditionalVisibility = getTotalCount(session, query);
        long excludedNodes =
                getTotalCount(session, queryInvalidStartEndDate)
                        - getTotalCount(session, queryInvalidStartEndWithDayOfWeek)
                        - getTotalCount(session, queryInvalidStartEndWithTimeOfDay)
                        - getTotalCount(session, queryInvalidStartEndWithDayOfWeekAndTimeOfDay);

        totalContent = totalNumOfNodesWithConditionalVisibility - excludedNodes;
        fillReport(session, query, offset, limit);
    }

    @Override public void addItem(JCRNodeWrapper node) throws RepositoryException {
        Map<String, String> liveConditions = conditionService.getConditions(node);
        List<String> conditions = liveConditions.entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase(IS_CONDITION_MATCHED) &&
                        !entry.getKey().equalsIgnoreCase(CURRENT_STATUS))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        if (!conditions.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            map.put("name", node.getName());
            map.put("path", node.getParent().getPath());
            map.put("type", String.join("<br/>", node.getNodeTypes()));
            map.put(CURRENT_STATUS_PROP, liveConditions.getOrDefault(CURRENT_STATUS_PROP, "not visible"));
            map.put(LIST_OF_CONDITIONS_PROP, String.join("<br/>", conditions));
            map.put(IS_CONDITION_MATCHED_PROP, liveConditions.getOrDefault(IS_CONDITION_MATCHED_PROP, "false"));
            this.dataList.add(map);
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
            item.put(nodeMap.get(LIST_OF_CONDITIONS_PROP));
            item.put(nodeMap.get(IS_CONDITION_MATCHED_PROP));
            item.put(nodeMap.get(CURRENT_STATUS_PROP));
            jArray.put(item);
        }

        return buildJsonResponse(totalContent, jArray);
    }

}
