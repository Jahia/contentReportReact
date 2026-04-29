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
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.NodeIterator;
import java.util.*;

/**
 * ReportAclInheritanceStopped Class.
 * <p>
 * Created by Juan Carlos Rodas.
 */
public class ReportAclInheritanceStopped extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportAclInheritanceStopped.class);
    protected static final String BUNDLE = "resources.contentReportReact";
    private long totalContent;

    /**
     * Instantiates a new Report pages without title.
     *
     * @param siteNode the site node
     */
    public ReportAclInheritanceStopped(JCRSiteNode siteNode) {
        super(siteNode);
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException {
        // Use a Set to track unique content node paths and avoid duplicates
        Set<String> processedPaths = new HashSet<>();
        
        // Query 1: ACL nodes with broken inheritance (j:inherit=false)
        String aclQuery = "SELECT * FROM [jnt:acl] AS item WHERE [j:inherit]=false AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        
        Query aclQueryObj = queryManager.createQuery(aclQuery, Query.JCR_SQL2);
        QueryResult aclResult = aclQueryObj.execute();
        NodeIterator aclIterator = aclResult.getNodes();
        
        while (aclIterator.hasNext()) {
            JCRNodeWrapper aclNode = (JCRNodeWrapper) aclIterator.next();
            JCRNodeWrapper contentNode = aclNode.getParent();
            if (contentNode != null && !processedPaths.contains(contentNode.getPath())) {
                addItem(aclNode);
                processedPaths.add(contentNode.getPath());
            }
        }
        
        // Query 2: ACE nodes with DENY permissions
        String aceQuery = "SELECT * FROM [jnt:ace] AS item WHERE [j:aceType]='DENY' AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        Query aceQueryObj = queryManager.createQuery(aceQuery, Query.JCR_SQL2);
        QueryResult aceResult = aceQueryObj.execute();
        NodeIterator aceIterator = aceResult.getNodes();
        
        while (aceIterator.hasNext()) {
            JCRNodeWrapper aceNode = (JCRNodeWrapper) aceIterator.next();
            // For ACE nodes: ace -> acl -> content node
            JCRNodeWrapper aclParent = aceNode.getParent();
            if (aclParent != null) {
                JCRNodeWrapper contentNode = aclParent.getParent();
                if (contentNode != null && !processedPaths.contains(contentNode.getPath())) {
                    // Create a pseudo-acl node context for addItem
                    addItem(aclParent);
                    processedPaths.add(contentNode.getPath());
                }
            }
        }
        
        totalContent = this.dataList.size();
    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {
        //adding the node to list if Acl Inheritance is Break
        node = node.getParent();
        Map<String, String> nodeMap = buildBaseNodeMap(node);
        nodeMap.put("expiration", node.getPropertyAsString("j:expiration"));
        nodeMap.put("nodeLockedBy", node.getLockOwner());
        nodeMap.put("nodeDisplayableName", WordUtils.abbreviate(node.getDisplayableName(),90,130,"..."));
        nodeMap.put("nodeTitle", (node.hasI18N(this.locale) && node.getI18N(this.defaultLocale).hasProperty("jcr:title")) ? node.getI18N(this.defaultLocale).getProperty("jcr:title").getString() : "");
        nodeMap.put("displayTitle", StringUtils.isNotEmpty(nodeMap.get("nodeTitle")) ? nodeMap.get("nodeTitle") : nodeMap.get("nodeName"));
        this.dataList.add(nodeMap);
    }

    public JSONObject getJson() throws JSONException, RepositoryException {

        JSONArray jArray = new JSONArray();

        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("nodeName"));
            item.put(nodeMap.get("nodePath"));
            jArray.put(item);        }

        return buildJsonResponse(totalContent, jArray);
    }



}
