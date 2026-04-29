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

import org.jahia.ajax.gwt.client.data.workflow.GWTJahiaWorkflow;
import org.jahia.ajax.gwt.client.data.workflow.GWTJahiaWorkflowDefinition;
import org.jahia.ajax.gwt.helper.WorkflowHelper;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.workflow.Workflow;
import org.jahia.services.workflow.WorkflowAction;
import org.jahia.services.workflow.WorkflowService;
import org.jahia.services.workflow.WorkflowTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * The ReportContentWaitingPublication Class.
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportContentWaitingPublication extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportContentWaitingPublication.class);
    protected static final String BUNDLE = "resources.contentReportReact";
    private long totalContent;
    List<WaitingPublicationElement> waitingPublicationList;


    /**
     * Instantiates a new Report pages without title.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportContentWaitingPublication(JCRSiteNode siteNode) {
        super(siteNode);
        this.waitingPublicationList  = new ArrayList<>();
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        String pageQueryStr = "SELECT * FROM [jmix:workflow] AS item WHERE [j:processId] is not null and ISDESCENDANTNODE(item,['" + siteNode.getPath() + "'])";
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
            waitingPublicationList.add(new WaitingPublicationElement(node, localeMap));
    }

    /**
     * getJson
     *
     * @return {@link JSONObject}
     * @throws JSONException
     * @throws RepositoryException
     */
    public JSONObject getJson() throws JSONException, RepositoryException {

        JSONArray jArray = new JSONArray();
        JSONArray jsonArrayItem;

        JSONArray jArray2;
        JSONObject jsonObjectItem2;

        for (WaitingPublicationElement element : this.waitingPublicationList) {
            jsonArrayItem = new JSONArray();
            jArray2 = new JSONArray();
            jsonArrayItem.put(element.getName());

            jsonArrayItem.put(element.getType());
            jsonArrayItem.put(element.getPath());

            for (String key: element.getElementMap().keySet()) {
                jsonArrayItem.put(element.getElementMap().get(key).get("wfStarted"));
                jsonArrayItem.put(element.getElementMap().get(key).get("wfDName"));
                jsonArrayItem.put(element.getElementMap().get(key).get("wfName"));
                for (String key2: element.getElementMap().get(key).keySet()) {
                    jsonArrayItem.put( element.getElementMap().get(key).get(key2));
                }

            }
            jArray.put(jsonArrayItem);
        }

        return buildJsonResponse(totalContent, jArray);
    }

    /* WaitingPublicationElement Class.*/
    class WaitingPublicationElement{

        private String path;
        private String url;
        private String title;
        private String name;
        private String type;
        private String techName;
        private Map<String, Map<String, String>> elementMap;
        private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


        /* the class constructor. */
        WaitingPublicationElement(JCRNodeWrapper node, Map<String, Locale> localeMap) throws RepositoryException {
            this.elementMap = new HashMap();
            this.path  = node.getPath() ;
            this.url   = node.getUrl();
            this.title = (node.hasI18N(defaultLocale) && node.getI18N(defaultLocale).hasProperty("jcr:title")) ? node.getI18N(defaultLocale).getProperty("jcr:title").getString() : node.getName();
            this.name  = node.getName();
            this.type  = node.getPrimaryNodeTypeName().split(":")[1];
            this.techName = node.getPrimaryNodeTypeName();

            for (String lang : localeMap.keySet()) {
                logger.info(node + "###" +  lang + "###" + localeMap.get(lang));
                List<Workflow> wfList = WorkflowService.getInstance().getActiveWorkflows(node, localeMap.get(lang), localeMap.get(lang));

                this.elementMap.put(lang, new HashMap());
                this.elementMap.get(lang).put("wfStarted", getWorkflowData( wfList, "startTime"));
                this.elementMap.get(lang).put("wfName", getWorkflowData( wfList, "name"));
                this.elementMap.get(lang).put("wfDName", getWorkflowData( wfList, "displayableName"));
                this.elementMap.get(lang).put("wfStartUser", getWorkflowData( wfList, "startUser"));
                this.elementMap.get(lang).put("wfProvider", getWorkflowData( wfList, "provider"));
                this.elementMap.get(lang).put("wfComments", getWorkflowData( wfList, "comments"));
            }
        }

        /**
         * getWorkflowData
         * <p>return the information by workflow as string.</p>
         *
         * @param wList
         * @param field
         * @return
         */
        private String getWorkflowData(List<Workflow> wList, String field){
            StringBuilder sb = new StringBuilder();
            HashMap<String, String> displayableName = new HashMap<>();

            if(wList != null) {
                for (Workflow wf : wList) {
                    if (sb.length() > 0) { sb.append(","); }
                    if(field.equalsIgnoreCase("startTime")) {
                        sb.append(dateFormat.format(wf.getStartTime()));
                        break;
                    }
                    if(field.equalsIgnoreCase("name")) {
                        sb.append(wf.getName());
                        break;
                    }
                    if(field.equalsIgnoreCase("displayableName")) {
                        WorkflowHelper wh = new WorkflowHelper();
                        GWTJahiaWorkflow gwtWD = wh.getGWTJahiaWorkflow(wf);
                        displayableName.put(wf.getId(), gwtWD.getVariables().get("jcr_title").getValues().toArray()[0].toString());
                    }
                    if(field.equalsIgnoreCase("startUser")) {
                        sb.append(wf.getStartUser());
                        break;
                    }
                    if(field.equalsIgnoreCase("provider")) {
                        sb.append(wf.getProvider());
                        break;
                    }
                    if(field.equalsIgnoreCase("comments")) {
                        sb.append(wf.getComments());
                        break;
                    }
                }
            }

            if(field.equalsIgnoreCase("displayableName")) {
                for (String key : displayableName.keySet()) {
                    sb.append(displayableName.get(key) + "<br />");
                }
            }
            return sb.toString();
        }

        /**
         * Getter for property 'path'.
         *
         * @return Value for property 'path'.
         */
        public String getPath() {
            return path;
        }

        /**
         * Setter for property 'path'.
         *
         * @param path Value to set for property 'path'.
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         * Getter for property 'url'.
         *
         * @return Value for property 'url'.
         */
        public String getUrl() {
            return url;
        }

        /**
         * Setter for property 'url'.
         *
         * @param url Value to set for property 'url'.
         */
        public void setUrl(String url) {
            this.url = url;
        }

        /**
         * Getter for property 'title'.
         *
         * @return Value for property 'title'.
         */
        public String getTitle() {
            return title;
        }

        /**
         * Setter for property 'title'.
         *
         * @param title Value to set for property 'title'.
         */
        public void setTitle(String title) {
            this.title = title;
        }

        /**
         * Getter for property 'name'.
         *
         * @return Value for property 'name'.
         */
        public String getName() {
            return name;
        }

        /**
         * Setter for property 'name'.
         *
         * @param name Value to set for property 'name'.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Getter for property 'type'.
         *
         * @return Value for property 'type'.
         */
        public String getType() {
            return type;
        }

        /**
         * Setter for property 'type'.
         *
         * @param type Value to set for property 'type'.
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Getter for property 'techName'.
         *
         * @return Value for property 'techName'.
         */
        public String getTechName() {
            return techName;
        }

        /**
         * Setter for property 'techName'.
         *
         * @param techName Value to set for property 'techName'.
         */
        public void setTechName(String techName) {
            this.techName = techName;
        }


        /**
         * Getter for property 'elementMap'.
         *
         * @return Value for property 'elementMap'.
         */
        public Map<String, Map<String, String>> getElementMap() {
            return elementMap;
        }

        /**
         * Setter for property 'elementMap'.
         *
         * @param elementMap Value to set for property 'elementMap'.
         */
        public void setElementMap(Map<String, Map<String, String>> elementMap) {
            this.elementMap = elementMap;
        }
    }

}
