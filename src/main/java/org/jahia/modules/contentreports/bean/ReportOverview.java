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

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.exceptions.JahiaException;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.query.QueryWrapper;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;

/**
 * The ReportOverview Class
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportOverview extends BaseReport {
    private static Logger logger = LoggerFactory.getLogger(ReportOverview.class);
    protected static final String BUNDLE = "resources.contentReportReact";

    private Integer pagesNumber;
    private Integer templatesNumber;
    private Integer usersNumber;
    private Integer contentsNumber;
    private Integer editorialContentsNumber;
    private Integer workflowTasksNumber;
    private Integer filesNumber;
    private Integer imagesNumber;
    private List<String> languages;
    
    // Content Activity metrics (last 30 days)
    private Integer newContentLast30Days;
    private Integer modifiedContentLast30Days;
    private Integer publishedContentLast30Days;
    private Integer unpublishedNodes;
    private Integer publishedNodes;
    private Double averageTimeToPublish; // in days
    private List<Map<String, Object>> topContributors;


    /**
     * Instantiates a new Report overview.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportOverview(JCRSiteNode siteNode) {
        super(siteNode);

        this.pagesNumber     = 0;
        this.templatesNumber = 0;
        this.usersNumber     = 0;
        this.contentsNumber  = 0;
        this.editorialContentsNumber = 0;
        this.workflowTasksNumber = 0;
        this.filesNumber = 0;
        this.imagesNumber = 0;
        this.languages = new ArrayList<>();
        
        // Initialize activity metrics
        this.newContentLast30Days = 0;
        this.modifiedContentLast30Days = 0;
        this.publishedContentLast30Days = 0;
        this.unpublishedNodes = 0;
        this.publishedNodes = 0;
        this.averageTimeToPublish = 0.0;
        this.topContributors = new ArrayList<>();
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        /* getting the templates for site */
        JahiaTemplateManagerService templateService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
        List<JahiaTemplatesPackage>  tpack = templateService.getInstalledModulesForSite(siteNode.getSiteKey(), true, true, false);
        this.templatesNumber = tpack.size();

        /* getting the users for site */
        JahiaUserManagerService userService = ServicesRegistry.getInstance().getJahiaUserManagerService();
        List<String> uList = userService.getUserList(siteNode.getSiteKey());
        this.usersNumber = uList.size();

        /* getting the pages count */
        String pageQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jnt:page] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper q = session.getWorkspace().getQueryManager().createQuery(pageQueryStr, Query.JCR_SQL2);
        this.pagesNumber = (int) q.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the jnt:content count */
        String contentQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jnt:content] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper contentQuery = session.getWorkspace().getQueryManager().createQuery(contentQueryStr, Query.JCR_SQL2);
        this.contentsNumber = (int) contentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the jmix:editorialContent count */
        String editorialQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:editorialContent] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper editorialQuery = session.getWorkspace().getQueryManager().createQuery(editorialQueryStr, Query.JCR_SQL2);
        this.editorialContentsNumber = (int) editorialQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the pending workflow tasks count - nodes with workflow processes */
        String workflowQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:workflow] AS item WHERE [j:processId] is not null AND ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper workflowQuery = session.getWorkspace().getQueryManager().createQuery(workflowQueryStr, Query.JCR_SQL2);
        this.workflowTasksNumber = (int) workflowQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the files count (all assets) */
        String filesQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jnt:file] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper filesQuery = session.getWorkspace().getQueryManager().createQuery(filesQueryStr, Query.JCR_SQL2);
        this.filesNumber = (int) filesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the images count */
        String imagesQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:image] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
        QueryWrapper imagesQuery = session.getWorkspace().getQueryManager().createQuery(imagesQueryStr, Query.JCR_SQL2);
        this.imagesNumber = (int) imagesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting the site languages */
        Set<String> languageSet = siteNode.getLanguages();
        if (languageSet != null && !languageSet.isEmpty()) {
            this.languages = new ArrayList<>(languageSet);
            Collections.sort(this.languages);
        }
        
        // Calculate Content Activity metrics (last 30 days)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        String thirtyDaysAgo = dateFormat.format(cal.getTime());
        
        /* getting new content in last 30 days */
        String newContentQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:editorialContent] AS item " +
                "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "']) " +
                "AND [jcr:created] >= CAST('" + thirtyDaysAgo + "T00:00:00.000Z' AS DATE)";
        QueryWrapper newContentQuery = session.getWorkspace().getQueryManager().createQuery(newContentQueryStr, Query.JCR_SQL2);
        this.newContentLast30Days = (int) newContentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting modified content in last 30 days */
        String modifiedContentQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:editorialContent] AS item " +
                "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "']) " +
                "AND [jcr:lastModified] >= CAST('" + thirtyDaysAgo + "T00:00:00.000Z' AS DATE)";
        QueryWrapper modifiedContentQuery = session.getWorkspace().getQueryManager().createQuery(modifiedContentQueryStr, Query.JCR_SQL2);
        this.modifiedContentLast30Days = (int) modifiedContentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting published content in last 30 days (nodes with j:lastPublished in last 30 days) */
        String publishedContentQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:lastPublished] AS item " +
                "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "']) " +
                "AND [j:lastPublished] >= CAST('" + thirtyDaysAgo + "T00:00:00.000Z' AS DATE)";
        QueryWrapper publishedContentQuery = session.getWorkspace().getQueryManager().createQuery(publishedContentQueryStr, Query.JCR_SQL2);
        this.publishedContentLast30Days = (int) publishedContentQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        /* getting unpublished vs published nodes count */
        String publishedNodesQueryStr = "SELECT [rep:count(item,skipChecks=1)] FROM [jmix:editorialContent] AS item " +
                "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "']) " +
                "AND [j:published] = true";
        QueryWrapper publishedNodesQuery = session.getWorkspace().getQueryManager().createQuery(publishedNodesQueryStr, Query.JCR_SQL2);
        this.publishedNodes = (int) publishedNodesQuery.execute().getRows().nextRow().getValue("count").getLong();
        
        // Unpublished = total editorial content - published
        this.unpublishedNodes = this.editorialContentsNumber - this.publishedNodes;
        
        /* Calculate average time from creation to publication */
        try {
            String avgTimeQueryStr = "SELECT item.[jcr:created] AS created, item.[j:lastPublished] AS published " +
                    "FROM [jmix:lastPublished] AS item " +
                    "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "']) " +
                    "AND item.[j:lastPublished] is not null " +
                    "AND item.[jcr:created] is not null";
            QueryWrapper avgTimeQuery = session.getWorkspace().getQueryManager().createQuery(avgTimeQueryStr, Query.JCR_SQL2);
            javax.jcr.query.RowIterator rows = avgTimeQuery.execute().getRows();
            
            long totalDiff = 0;
            int count = 0;
            while (rows.hasNext()) {
                javax.jcr.query.Row row = rows.nextRow();
                try {
                    Calendar created = row.getValue("created").getDate();
                    Calendar published = row.getValue("published").getDate();
                    long diffInMillis = published.getTimeInMillis() - created.getTimeInMillis();
                    long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
                    if (diffInDays >= 0) { // Only positive differences
                        totalDiff += diffInDays;
                        count++;
                    }
                } catch (Exception e) {
                    // Skip rows with invalid dates
                }
            }
            
            if (count > 0) {
                this.averageTimeToPublish = (double) totalDiff / count;
            }
        } catch (Exception e) {
            logger.warn("Error calculating average time to publish", e);
            this.averageTimeToPublish = 0.0;
        }
        
        /* Getting top contributors (authors/editors) */
        try {
            // Query all editorial content and aggregate by author manually
            String topContributorsQueryStr = "SELECT item.[jcr:createdBy] AS author " +
                    "FROM [jmix:editorialContent] AS item " +
                    "WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
            QueryWrapper topContributorsQuery = session.getWorkspace().getQueryManager().createQuery(topContributorsQueryStr, Query.JCR_SQL2);
            javax.jcr.query.RowIterator rows = topContributorsQuery.execute().getRows();
            
            // Collect contributors with their counts manually
            Map<String, Integer> contributorMap = new HashMap<>();
            while (rows.hasNext()) {
                javax.jcr.query.Row row = rows.nextRow();
                try {
                    String author = row.getValue("author").getString();
                    if (author != null && !author.isEmpty()) {
                        contributorMap.put(author, contributorMap.getOrDefault(author, 0) + 1);
                    }
                } catch (Exception e) {
                    // Skip invalid rows
                    logger.debug("Skipping row without valid author", e);
                }
            }
            
            // Sort by count (descending) and get top 5
            List<Map.Entry<String, Integer>> sortedContributors = new ArrayList<>(contributorMap.entrySet());
            sortedContributors.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
            
            this.topContributors = new ArrayList<>();
            int topLimit = Math.min(5, sortedContributors.size());
            for (int i = 0; i < topLimit; i++) {
                Map.Entry<String, Integer> entry = sortedContributors.get(i);
                Map<String, Object> contributor = new HashMap<>();
                contributor.put("username", entry.getKey());
                contributor.put("contentCount", entry.getValue());
                this.topContributors.add(contributor);
            }
            
            logger.info("Found {} top contributors from {} total authors", topLimit, contributorMap.size());
        } catch (Exception e) {
            logger.error("Error calculating top contributors", e);
            this.topContributors = new ArrayList<>();
        }
    }

    /**
     * getJson
     *
     * @return {@link JSONObject}
     * @throws JSONException
     * @throws RepositoryException
     */
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("nbPages", pagesNumber);
        jsonObject.put("nbTemplates", templatesNumber);
        jsonObject.put("nbUsers", usersNumber);
        jsonObject.put("nbContents", contentsNumber);
        jsonObject.put("nbEditorialContents", editorialContentsNumber);
        jsonObject.put("nbWorkflowTasks", workflowTasksNumber);
        jsonObject.put("nbFiles", filesNumber);
        jsonObject.put("nbImages", imagesNumber);
        jsonObject.put("languages", languages);
        jsonObject.put("nbLanguages", languages.size());
        
        // Content Activity metrics
        jsonObject.put("newContentLast30Days", newContentLast30Days);
        jsonObject.put("modifiedContentLast30Days", modifiedContentLast30Days);
        jsonObject.put("publishedContentLast30Days", publishedContentLast30Days);
        jsonObject.put("unpublishedNodes", unpublishedNodes);
        jsonObject.put("publishedNodes", publishedNodes);
        jsonObject.put("averageTimeToPublish", averageTimeToPublish);
        jsonObject.put("topContributors", topContributors);
        
        return jsonObject;
    }

}
