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
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.jcr.RepositoryException;
import java.util.*;

/**
 * ReportBeforeDate Class.
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportBeforeDate extends QueryReport {

    private Map<String, Integer> dataMap;

    private String searchPath;
    private String strDate;
    private Boolean useSystemUser;

    /**
     * The ReportBeforeDate Constructor.
     *
     * @param useSystemUser
     */
    public ReportBeforeDate(JCRSiteNode siteNode, String path, String date, Boolean useSystemUser){
        super(siteNode);
        this.searchPath = path;
        this.strDate = date;
        this.dataMap = new HashMap<String, Integer>();
        this.useSystemUser = useSystemUser;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        String strQuery = "SELECT * FROM [jmix:editorialContent] AS item WHERE ISDESCENDANTNODE(item,['" + searchPath + "']) and item.[jcr:lastModified] <= CAST('" + strDate + "T23:59:59.999Z' AS DATE)";
        fillReport(session, strQuery, offset, limit);
    }

    /**
     * addItem
     *
     * @param node @{@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {
        String propertyName = "jcr:lastModifiedBy";
        Date itemDate = node.getLastModifiedAsDate();
        Map<String, String> nodeMap;

        if(itemDate != null){
            String userName = node.getPropertyAsString(propertyName);
            if(userName.equalsIgnoreCase("system") && !useSystemUser)
                return;

            Calendar calendar = new GregorianCalendar();
            calendar.setTime(itemDate);
            int month = calendar.get(Calendar.MONTH) + 1;
            nodeMap = new HashMap<>();

            nodeMap.put("date", dateFormat.format(itemDate));
            nodeMap.put("nodeName", node.hasProperty("jcr:title") ? node.getPropertyAsString("jcr:title") : node.getDisplayableName());
            nodeMap.put("type", node.getPrimaryNodeTypeName());
            nodeMap.put("typeName", node.getPrimaryNodeTypeName().split(":")[1]);

            dataList.add(nodeMap);


            /*setting the counter*/
            if(dataMap.containsKey(dateFormat.format(itemDate)))
                dataMap.put(dateFormat.format(itemDate), dataMap.get(dateFormat.format(itemDate)) + 1);
            else
                dataMap.put(dateFormat.format(itemDate), 1);
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
        JSONArray jsonArrayLabels = new JSONArray();;
        JSONArray jsonArrayValues = new JSONArray();

        /* filling the chart data */
        for (String dateKey: dataMap.keySet()) {
            jsonArrayLabels.put(dateKey);
            jsonArrayValues.put(dataMap.get(dateKey));
        }

        /* filling the table data */
        for (Map<String, String> itemMap :dataList){
            jArray.put(new JSONObject(itemMap));
        }

        jsonObject.put("items",jArray);
        jsonObject.put("chartLabels",jsonArrayLabels);
        jsonObject.put("chartValues",jsonArrayValues);

        return jsonObject;
    }

}
