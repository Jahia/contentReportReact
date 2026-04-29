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
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 * ReportByAuthor Class.
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportByAuthor extends QueryReport {

    protected final String PROPERTY_COUNT = "itemCount";
    protected final String PROPERTY_AUTHOR_DETAIL = "authorDetail";
    protected final String PROPERTY_PAGE_DETAIL = "pageDetail";
    protected Integer totalItems;
    protected Map<String, Map<String, Object>> dataMap;
    protected Boolean useSystemUser;
    protected String searchPath;
    protected SearchContentType reportType;
    protected SearchActionType actionType;
    protected String propertyName = "";

    /**
     * The ReportByAuthor constructor.
     *
     * @param reportType {@link SearchContentType}
     * @param actionType {@link SearchActionType}
     * @param useSystemUser {@link Boolean}
     */
    public ReportByAuthor(JCRSiteNode siteNode, String path, SearchContentType reportType, SearchActionType actionType, Boolean useSystemUser) {
        super(siteNode);
        this.searchPath = path;
        this.useSystemUser = useSystemUser;
        this.reportType = reportType;
        this.actionType = actionType;
        this.dataMap = new HashMap<>();
        this.totalItems = 0;
        if (actionType.equals(SearchActionType.CREATION))
            this.propertyName = "jcr:createdBy";
        else if (actionType.equals(SearchActionType.UPDATE))
            this.propertyName = "jcr:lastModifiedBy";
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        String strQuery = "SELECT * FROM ";
        strQuery += (reportType.equals(BaseReport.SearchContentType.PAGE) ? "[jnt:page] " : "[jmix:editorialContent] ");
        strQuery += "AS item WHERE ISDESCENDANTNODE(item,['" + searchPath + "'])";
        fillReport(session, strQuery, offset, limit);
    }

    /**
     * addItem
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {

        if (node.hasProperty(propertyName)) {
            String userName = node.getPropertyAsString(propertyName);
            if (userName.equalsIgnoreCase("system") && !useSystemUser)
                return;

            if (!getDataMap().containsKey(userName))
                getDataMap().put(userName, new HashedMap());

            /*setting the counter*/
            if (getDataMap().get(userName).containsKey(PROPERTY_COUNT)) {
                getDataMap().get(userName).put(PROPERTY_COUNT, ((Integer) getDataMap().get(userName).get(PROPERTY_COUNT) + 1));
            } else {
                getDataMap().get(userName).put(PROPERTY_COUNT, 1);
            }

            /*setting the author detail*/
            if (!getDataMap().get(userName).containsKey(PROPERTY_AUTHOR_DETAIL))
                getDataMap().get(userName).put(PROPERTY_AUTHOR_DETAIL, new ReportByAuthorDetail());

            ((ReportByAuthorDetail) getDataMap().get(userName).get(PROPERTY_AUTHOR_DETAIL)).addAuthorDetailItem(node);


             /*setting the page author detail*/
            if (!getDataMap().get(userName).containsKey(PROPERTY_PAGE_DETAIL))
                getDataMap().get(userName).put(PROPERTY_PAGE_DETAIL, new ReportByAuthorPageDetail());

            ((ReportByAuthorPageDetail) getDataMap().get(userName).get(PROPERTY_PAGE_DETAIL)).addAuthorPageDetailItem(node);


            setTotalItems(getTotalItems() + 1);
        }
    }

    /**
     * getJson
     *
     * @return {@link JSONObject}
     * @throws JSONException
     */
    public JSONObject getJson() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONArray jArray = new JSONArray();
        JSONObject jsonObjectItem;

        ReportByAuthorDetail authorDetail;
        JSONObject jsonObjectItemAuthorDetail;
        JSONArray jsonArrayItemAuthorDetail;
        JSONObject jsonObjectSubItemAuthorDetail;

        ReportByAuthorPageDetail authorPageDetail;
        JSONObject jsonObjectItemPageDetail;
        JSONArray jsonArrayItemPageDetail;
        JSONObject jsonObjectSubItemPageDetail;

        for (Map.Entry<String, Map<String, Object>> entry : dataMap.entrySet()) {
            String key = entry.getKey();
            Map<String, Object> dataEntry = entry.getValue();
            jsonObjectItem = new JSONObject();
            jsonObjectItem.put("user", key);
            jsonObjectItem.put("itemCount", dataEntry.get(PROPERTY_COUNT));
            jsonObjectItem.put("percentaje", (Float.parseFloat(((Integer) dataEntry.get(PROPERTY_COUNT) * 100) + "") / totalItems));

            /* part of author detail */
            authorDetail = (ReportByAuthorDetail) dataEntry.get(PROPERTY_AUTHOR_DETAIL);
            jsonObjectItemAuthorDetail = new JSONObject();
            jsonObjectItemAuthorDetail.put("totalCount", authorDetail.getTotalAuthorDetailItems());

            jsonArrayItemAuthorDetail = new JSONArray();
            for (Map.Entry<String, Integer> authorDetailEntry : authorDetail.getDetailAuthorMap().entrySet()) {
                jsonObjectSubItemAuthorDetail = new JSONObject();
                jsonObjectSubItemAuthorDetail.put("type", authorDetailEntry.getKey());
                jsonObjectSubItemAuthorDetail.put("typeName", (authorDetailEntry.getKey().split(":")[1]));
                jsonObjectSubItemAuthorDetail.put("itemCount", authorDetailEntry.getValue());
                jsonObjectSubItemAuthorDetail.put("percentaje", (Float.parseFloat((authorDetailEntry.getValue() * 100) + "") / authorDetail.getTotalAuthorDetailItems()));
                jsonArrayItemAuthorDetail.put(jsonObjectSubItemAuthorDetail);
            }
            jsonObjectItemAuthorDetail.put("items", jsonArrayItemAuthorDetail);
            jsonObjectItem.put("itemAuthorDetails", jsonObjectItemAuthorDetail);

            /* part of page details */
            authorPageDetail = (ReportByAuthorPageDetail) dataEntry.get(PROPERTY_PAGE_DETAIL);
            jsonObjectItemPageDetail = new JSONObject();
            jsonArrayItemPageDetail = new JSONArray();
            for (Map.Entry<String, Map<String, String>> pageDetailEntry : authorPageDetail.getDetailAuthorPageMap().entrySet()) {
                jsonObjectSubItemPageDetail = new JSONObject();
                jsonObjectSubItemPageDetail.put("title", pageDetailEntry.getKey());
                for (Map.Entry<String, String> pageDetailItemEntry : pageDetailEntry.getValue().entrySet()) {
                    jsonObjectSubItemPageDetail.put(pageDetailItemEntry.getKey(), pageDetailItemEntry.getValue());
                }
                jsonArrayItemPageDetail.put(jsonObjectSubItemPageDetail);
            }
            jsonObjectItemPageDetail.put("items", jsonArrayItemPageDetail);
            jsonObjectItem.put("itemAuthorPageDetails", jsonObjectItemPageDetail);


            /* setting each item to the json object */
            jArray.put(jsonObjectItem);
        }

        jsonObject.put("reportType", reportType.name());
        jsonObject.put("totalItems", totalItems);
        jsonObject.put("items", jArray);

        return jsonObject;
    }

    /**
     * getTotalItems
     *
     * @return {@link Integer}
     */
    public Integer getTotalItems() {
        return totalItems;
    }

    /**
     * setTotalItems
     *
     * @param totalItems {@link Integer}
     */
    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    /**
     * getDataMap
     *
     * @return {@link Map}
     */
    public Map<String, Map<String, Object>> getDataMap() {
        return dataMap;
    }

    /**
     * ReportByAuthorDetail Class.
     */
    public class ReportByAuthorDetail {

        private Integer totalAuthorDetailItems;
        private Map<String, Integer> detailAuthorMap;

        /* The constructor for the class. */
        public ReportByAuthorDetail() {
            this.setTotalAuthorDetailItems(0);
            this.setDetailAuthorMap(new HashMap<String, Integer>());
        }

        /**
         * addAuthorDetailItem
         * <p>add detail item for specific author.</p>
         *
         * @param detailNode {@link JCRNodeWrapper}
         * @throws RepositoryException
         */
        public void addAuthorDetailItem(JCRNodeWrapper detailNode) throws RepositoryException {
            String primaryNodeTypeName = detailNode.getPrimaryNodeTypeName();

            /*setting the counter*/
            if (getDetailAuthorMap().containsKey(primaryNodeTypeName)) {
                getDetailAuthorMap().put(primaryNodeTypeName, getDetailAuthorMap().get(primaryNodeTypeName) + 1);
            } else {
                getDetailAuthorMap().put(primaryNodeTypeName, 1);
            }
            setTotalAuthorDetailItems(getTotalAuthorDetailItems() + 1);
        }

        /**
         * getTotalAuthorDetailItems
         *
         * @return {@link Integer}
         */
        public Integer getTotalAuthorDetailItems() {
            return totalAuthorDetailItems;
        }

        /**
         * setTotalAuthorDetailItems
         *
         * @param totalAuthorDetailItems {@link Integer}
         */
        public void setTotalAuthorDetailItems(Integer totalAuthorDetailItems) {
            this.totalAuthorDetailItems = totalAuthorDetailItems;
        }

        /**
         * getDetailAuthorMap
         *
         * @return {@link Map}
         */
        public Map<String, Integer> getDetailAuthorMap() {
            return detailAuthorMap;
        }

        /**
         * setDetailAuthorMap
         *
         * @param detailAuthorMap {@link Map}
         */
        public void setDetailAuthorMap(Map<String, Integer> detailAuthorMap) {
            this.detailAuthorMap = detailAuthorMap;
        }
    }

    /**
     * ReportByAuthorPageDetail Class
     */
    public class ReportByAuthorPageDetail {

        private Integer totalAuthorPageItems;
        private Map<String, Map<String, String>> detailAuthorPageMap;

        /* the constructor for the class */
        public ReportByAuthorPageDetail() {
            this.setTotalAuthorPageItems(0);
            this.setDetailAuthorPageMap(new HashMap<String, Map<String, String>>());
        }

        /**
         * addAuthorPageDetailItem
         * <p>add detail item for specific author page item.</p>
         *
         * @param detailNode {@link JCRNodeWrapper}
         * @throws RepositoryException
         */
        public void addAuthorPageDetailItem(JCRNodeWrapper detailNode) throws RepositoryException {
            String nodeTitle = detailNode.getName();

            /*setting the key if not exists*/
            if (!getDetailAuthorPageMap().containsKey(nodeTitle))
                getDetailAuthorPageMap().put(nodeTitle, new HashedMap());

            /*setting the data from the node*/
            getDetailAuthorPageMap().get(nodeTitle).put("jcrtitle", detailNode.hasProperty("jcr:title") ? detailNode.getPropertyAsString("jcr:title") : nodeTitle);
            getDetailAuthorPageMap().get(nodeTitle).put("type", detailNode.getPrimaryNodeTypeName());
            getDetailAuthorPageMap().get(nodeTitle).put("typeName", detailNode.getPrimaryNodeTypeName().split(":")[1]);
            getDetailAuthorPageMap().get(nodeTitle).put("created", detailNode.hasProperty("jcr:created") ? detailNode.getPropertyAsString("jcr:created") : "");
            getDetailAuthorPageMap().get(nodeTitle).put("modified", detailNode.hasProperty("jcr:lastModified") ? detailNode.getPropertyAsString("jcr:lastModified") : "");
            getDetailAuthorPageMap().get(nodeTitle).put("published", detailNode.hasProperty("j:published") ? detailNode.getPropertyAsString("j:published") : "false");
            getDetailAuthorPageMap().get(nodeTitle).put("locked", detailNode.hasProperty("j:locktoken") ? "true" : "false");
        }

        /**
         * getTotalAuthorPageItems
         *
         * @return {@link Integer}
         */
        public Integer getTotalAuthorPageItems() {
            return totalAuthorPageItems;
        }

        /**
         * setTotalAuthorPageItems
         *
         * @param totalAuthorPageItems {@link Integer}
         */
        public void setTotalAuthorPageItems(Integer totalAuthorPageItems) {
            this.totalAuthorPageItems = totalAuthorPageItems;
        }

        /**
         * getDetailAuthorPageMap
         *
         * @return {@link Map}
         */
        public Map<String, Map<String, String>> getDetailAuthorPageMap() {
            return detailAuthorPageMap;
        }

        /**
         * setDetailAuthorPageMap
         *
         * @param detailAuthorPageMap {@link Map}
         */
        public void setDetailAuthorPageMap(Map<String, Map<String, String>> detailAuthorPageMap) {
            this.detailAuthorPageMap = detailAuthorPageMap;
        }
    }


}
