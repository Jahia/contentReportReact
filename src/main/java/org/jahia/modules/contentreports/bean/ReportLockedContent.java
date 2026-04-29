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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * The ReportLockedContent Class.
 * <p>
 * Created by Juan Carlos Rodas.
 */
public class ReportLockedContent extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportLockedContent.class);
    protected static final String BUNDLE = "resources.contentReportReact";
    private long totalContent;
    private int sortCol;
    private String order;
    private String[] resultFields = {"j:nodename", "jcr:primaryType", "jcr:createdBy", "jcr:lockOwner", "j:nodename"};

    /**
     * Instantiates a new Report pages without title.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportLockedContent(JCRSiteNode siteNode, int sortCol, String order) {
        super(siteNode);
        this.sortCol = sortCol;
        this.order = order;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException {
        String orderStatement = " order by item.[" + resultFields[sortCol] + "] " + order;
        String pageQueryStr = "SELECT * FROM [jmix:editorialContent] AS item WHERE [j:lockTypes] is not null and ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])" + orderStatement;
        fillReport(session, pageQueryStr, offset, limit);
        totalContent = getTotalCount(session, pageQueryStr);
    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {

        JCRNodeWrapper itemParentPage = node;
        if (!node.isNodeType("jnt:page")) {
            itemParentPage = JCRContentUtils.getParentOfType(node, "jnt:page");
        }
        Map<String, String> nodeMap = buildBaseNodeMap(node);
        nodeMap.put("nodeLockedBy", node.getPropertyAsString("jcr:lockOwner"));
        if (itemParentPage != null) {
            nodeMap.put("nodeUsedInPageName", itemParentPage.getName());
            nodeMap.put("nodeUsedInPageDisplayableName", itemParentPage.getDisplayableName());
            nodeMap.put("nodeUsedInPagePath", itemParentPage.getPath());
            nodeMap.put("nodeUsedInPageUrl", itemParentPage.getUrl());
            nodeMap.put("nodeUsedInPageTitle", (itemParentPage.hasI18N(this.locale) && itemParentPage.getI18N(this.defaultLocale).hasProperty("jcr:title")) ? itemParentPage.getI18N(this.defaultLocale).getProperty("jcr:title").getString() : "");
        }
        nodeMap.put("nodeDisplayableName", WordUtils.abbreviate(node.getDisplayableName(),90,130,"..."));
        nodeMap.put("nodeTitle", (node.hasI18N(this.locale) && node.getI18N(this.defaultLocale).hasProperty("jcr:title")) ? node.getI18N(this.defaultLocale).getProperty("jcr:title").getString() : "");
        nodeMap.put("displayTitle", StringUtils.isNotEmpty(nodeMap.get("nodeTitle")) ? nodeMap.get("nodeTitle") : nodeMap.get("nodeName"));
        this.dataList.add(nodeMap);
    }

    @Override
    public JSONObject getJson() throws JSONException, RepositoryException {

        JSONArray jArray = new JSONArray();

        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("nodeName"));
            item.put(nodeMap.get("nodeType"));
            item.put(nodeMap.get("nodeAuthor"));
            item.put(nodeMap.get("nodeLockedBy"));
            item.put(nodeMap.get("nodeUsedInPagePath"));
            jArray.put(item);
        }
        return buildJsonResponse(totalContent, jArray);
    }


}
