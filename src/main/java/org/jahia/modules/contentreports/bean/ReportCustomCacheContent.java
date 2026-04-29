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

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
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
 * The ReportCustomCacheContent Class.
 * <p>
 * Created by Juan Carlos Rodas.
 */
public class ReportCustomCacheContent extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportCustomCacheContent.class);
    protected static final String BUNDLE = "resources.contentReportReact";
    private long totalContent;
    private int sortCol;
    private String order;
    private String[] resultFields = {"j:nodename", "jcr:primaryType", "j:expiration", "j:nodename"};


    /**
     * Instantiates a new Report pages without title.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportCustomCacheContent(JCRSiteNode siteNode, int sortCol, String order) {
        super(siteNode);
        this.sortCol = sortCol;
        this.order = order;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException {
        String orderStatement = " order by item.["+resultFields[sortCol]+"] " + order;
        String queryStr = "SELECT * FROM [jnt:content] AS item WHERE item.[j:expiration] IS NOT NULL AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])" + orderStatement;
        fillReport(session, queryStr, offset, limit);
        totalContent = getTotalCount(session, queryStr);

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

        Map<String, String> nodeMap = new HashedMap();
        nodeMap.put("nodePath", node.getPath());
        nodeMap.put("nodeUrl ", node.getUrl());
        nodeMap.put("nodeName", node.getName());
        nodeMap.put("nodeType", node.getPrimaryNodeTypeName());
        nodeMap.put("expiration", node.getPropertyAsString("j:expiration"));
        nodeMap.put("nodeTypeTechName", node.getPrimaryNodeTypeName().split(":")[1]);
        nodeMap.put("nodeTypeName", node.getPrimaryNodeType().getName());
        nodeMap.put("nodeTypePrefix", node.getPrimaryNodeType().getPrefix());
        nodeMap.put("nodeTypePrefix", node.getPrimaryNodeType().getPrefix());
        nodeMap.put("nodeTypeAlias", node.getPrimaryNodeType().getAlias());
        nodeMap.put("nodeAuthor", node.getCreationUser());
        nodeMap.put("nodeLockedBy", node.getLockOwner());
        nodeMap.put("nodeUsedInPageName", itemParentPage.getName());
        nodeMap.put("nodeUsedInPageDisplayableName", itemParentPage.getDisplayableName());
        nodeMap.put("nodeUsedInPagePath", itemParentPage.getPath());
        nodeMap.put("nodeUsedInPageUrl", itemParentPage.getUrl());
        nodeMap.put("nodeUsedInPageTitle", (itemParentPage.hasI18N(this.locale) && itemParentPage.getI18N(this.defaultLocale).hasProperty("jcr:title")) ? itemParentPage.getI18N(this.defaultLocale).getProperty("jcr:title").getString() : "");
        nodeMap.put("nodeDisplayableName", node.getDisplayableName());
        nodeMap.put("nodeTitle", (node.hasI18N(this.locale) && node.getI18N(this.defaultLocale).hasProperty("jcr:title")) ? node.getI18N(this.defaultLocale).getProperty("jcr:title").getString() : "");
        nodeMap.put("displayTitle", StringUtils.isNotEmpty(nodeMap.get("nodeTitle")) ? nodeMap.get("nodeTitle") : nodeMap.get("nodeName"));
        this.dataList.add(nodeMap);
    }

    public JSONObject getJson() throws JSONException, RepositoryException {

        JSONArray jArray = new JSONArray();

        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("nodeName"));
            item.put(nodeMap.get("nodeType"));
            item.put(nodeMap.get("expiration"));
            item.put(nodeMap.get("nodeUsedInPagePath"));
            jArray.put(item);
        }

        return buildJsonResponse(totalContent, jArray);
    }


}
