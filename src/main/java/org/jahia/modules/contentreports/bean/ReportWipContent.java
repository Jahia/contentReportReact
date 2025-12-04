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
import org.apache.commons.lang.WordUtils;
import org.jahia.api.Constants;
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
import java.util.Arrays;
import java.util.Map;

/**
 * Created by Francois Pral.
 */
public class ReportWipContent extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportWipContent.class);
    protected static final String BUNDLE = "resources.contentReportReact";
    private long totalContent;
    private int sortCol;
    private String searchPath;
    private String order;
    private String[] resultFields = {"j:nodename", "jcr:primaryType", "jcr:createdBy", "j:workInProgressStatus", "j:nodename"};
    private SearchContentType reportType;

    /**
     * Instantiates a new Report pages without title.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportWipContent(JCRSiteNode siteNode, String searchPath, SearchContentType reportType, int sortCol, String order) {
        super(siteNode);
        this.sortCol = sortCol;
        this.order = order;
        this.searchPath = searchPath;
        this.reportType = reportType;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException {

        String orderStatement = " order by item.[" + resultFields[sortCol] + "] " + order;
        String strQuery = "SELECT * FROM ";
        strQuery += (reportType.equals(BaseReport.SearchContentType.PAGE) ? "[jnt:page] " : "[jnt:content] ");
        strQuery += "AS item WHERE [j:workInProgressStatus] is not null  and [j:workInProgressStatus]<> \""+ Constants.WORKINPROGRESS_STATUS_DISABLED +"\" and ISDESCENDANTNODE(item,['" + searchPath + "'])" + orderStatement;

        fillReport(session, strQuery, offset, limit);
        totalContent = getTotalCount(session, strQuery);
    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {

        Map<String, String> nodeMap = new HashedMap();
        JCRNodeWrapper itemParentPage = node;
        if (!node.isNodeType("jnt:page")) {
            itemParentPage = JCRContentUtils.getParentOfType(node, "jnt:page");
        }
        if (itemParentPage != null) {
            nodeMap.put("nodeUsedInPagePath", itemParentPage.getPath());
        }

        nodeMap.put("nodeDisplayableName", WordUtils.abbreviate(node.getDisplayableName(),90,130,"..."));
        nodeMap.put("nodePath", node.getPath());
        nodeMap.put("nodeTypeName", node.getPrimaryNodeType().getLabel(this.defaultLocale));
        String WIPStatus = node.getPropertyAsString(Constants.WORKINPROGRESS_STATUS);
        if (WIPStatus.equals(Constants.WORKINPROGRESS_STATUS_LANG)){
            nodeMap.put("nodeWip", Arrays.toString(Arrays.asList(node.getProperty(Constants.WORKINPROGRESS_LANGUAGES).getValues()).stream().map(s -> "\"" + s + "\"").toArray()));
        } else {
            nodeMap.put("nodeWip", "[\"" + WIPStatus + "\"]");
        }

        this.dataList.add(nodeMap);
    }

    @Override
    public JSONObject getJson() throws JSONException, RepositoryException {

        JSONObject jsonObject = new JSONObject();
        JSONArray jArray = new JSONArray();

        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("nodeDisplayableName"));
            item.put(nodeMap.get("nodeTypeName"));
            item.put(nodeMap.get("nodeWip"));
            item.put(nodeMap.get("nodePath"));
            item.put(nodeMap.get("nodeUsedInPagePath"));
            jArray.put(item);
        }
        jsonObject.put("recordsTotal", totalContent);
        jsonObject.put("recordsFiltered", totalContent);
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("data", jArray);
        return jsonObject;
    }


}
