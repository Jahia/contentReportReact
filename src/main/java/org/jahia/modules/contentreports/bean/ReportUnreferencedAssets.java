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

import org.apache.commons.lang3.StringUtils;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

/**
 * Report listing assets that have no references in the repository.
 */
public class ReportUnreferencedAssets extends BaseReport {
    private static final Logger logger = LoggerFactory.getLogger(ReportUnreferencedAssets.class);

    private final String assetsRootPath;
    private final JSONArray dataList = new JSONArray();
    private long totalUnreferenced = 0;
    private int offsetCursor;
    private int remainingLimit;

    public ReportUnreferencedAssets(JCRSiteNode siteNode, String assetsRootPath) {
        super(siteNode);
        String defaultFilesPath = siteNode != null ? siteNode.getPath() + "/files" : "";
        this.assetsRootPath = StringUtils.isNotBlank(assetsRootPath) ? assetsRootPath : defaultFilesPath;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        this.offsetCursor = Math.max(0, offset);
        this.remainingLimit = (limit > 0) ? limit : Integer.MAX_VALUE;

        if (StringUtils.isBlank(assetsRootPath)) {
            logger.warn("No assets root path provided for unreferenced assets report");
            return;
        }

        if (!session.nodeExists(assetsRootPath)) {
            logger.warn("Assets root path {} does not exist", assetsRootPath);
            return;
        }

        JCRNodeWrapper root = (JCRNodeWrapper) session.getNode(assetsRootPath);
        collectAssets(root);
    }

    private void collectAssets(JCRNodeWrapper node) throws RepositoryException, JSONException {
        if (node.isNodeType("jnt:file")) {
            handleFile(node);
        }

        NodeIterator children = node.getNodes();
        while (children.hasNext()) {
            JCRNodeWrapper child = (JCRNodeWrapper) children.nextNode();
            // Skip the binary sub node, it never contains nested files
            if ("jcr:content".equals(child.getName())) {
                continue;
            }
            collectAssets(child);
        }
    }

    private void handleFile(JCRNodeWrapper fileNode) throws RepositoryException, JSONException {
        if (hasReferences(fileNode)) {
            return;
        }

        totalUnreferenced++;
        if (offsetCursor > 0) {
            offsetCursor--;
            return;
        }

        if (remainingLimit <= 0) {
            return;
        }

        String mimeType = "";
        if (fileNode.hasNode("jcr:content")) {
            JCRNodeWrapper contentNode = fileNode.getNode("jcr:content");
            if (contentNode.hasProperty("jcr:mimeType")) {
                mimeType = contentNode.getProperty("jcr:mimeType").getString();
            }
        }
        if (StringUtils.isBlank(mimeType) && fileNode.hasProperty("jcr:mimeType")) {
            mimeType = fileNode.getProperty("jcr:mimeType").getString();
        }

        JSONArray row = new JSONArray();
        row.put(mimeType);
        row.put(fileNode.getDisplayableName());
        row.put(fileNode.getPath());
        row.put(fileNode.getCreationUser());
        row.put(fileNode.hasProperty("jcr:created") ? fileNode.getProperty("jcr:created").getString() : "");
        row.put(fileNode.hasProperty("jcr:lastModified") ? fileNode.getProperty("jcr:lastModified").getString() : "");
        dataList.put(row);
        remainingLimit--;
    }

    private boolean hasReferences(JCRNodeWrapper node) throws RepositoryException {
        if (!node.isNodeType("mix:referenceable")) {
            return false;
        }

        PropertyIterator strongRefs = node.getReferences();
        if (strongRefs != null && strongRefs.hasNext()) {
            return true;
        }

        PropertyIterator weakRefs = node.getWeakReferences();
        return weakRefs != null && weakRefs.hasNext();
    }

    @Override
    public JSONObject getJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("recordsTotal", totalUnreferenced);
        jsonObject.put("recordsFiltered", totalUnreferenced);
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("data", dataList);
        return jsonObject;
    }
}
