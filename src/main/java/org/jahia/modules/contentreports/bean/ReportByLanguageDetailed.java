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

import org.apache.commons.lang.WordUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import javax.jcr.*;
import java.util.*;

/**
 * ReportByLanguageDetailed Class.
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportByLanguageDetailed extends QueryReport {

    private List<Map<String, String>> listMap;
    private Map<String, String> itemMap;
    private NodeLangInformation langInformation;
    private String language;

    /**
     * The constructor for the class.
     *
     * @param siteNode {@link JCRSiteNode}
     * @param language {@link String}
     */
    public ReportByLanguageDetailed(JCRSiteNode siteNode, String language) {
        super(siteNode);
        this.listMap = new ArrayList<>();
        this.language = language;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        fillReport(session, "SELECT item.* FROM [jmix:editorialContent] AS item WHERE ISDESCENDANTNODE(item,['" + siteNode.getPath() + "']) ORDER BY item.date ", offset, limit);
    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {
        this.langInformation = getLanguageInformation(node);

        /* adding the item information */
        this.itemMap = new HashMap<>();
        this.itemMap.put("title", node.hasProperty("jcr:title") ? node.getPropertyAsString("jcr:title") : "");
        this.itemMap.put("displayableName", WordUtils.abbreviate(node.getDisplayableName(),90,130,"..."));
        this.itemMap.put("name", node.getName());
        this.itemMap.put("displayTitle", node.hasProperty("jcr:title") ? node.getPropertyAsString("jcr:title") : node.getDisplayableName());
        this.itemMap.put("primaryNodeTypeAlias", node.getPrimaryNodeType().getAlias());
        this.itemMap.put("primaryNodeTypeLocalName", node.getPrimaryNodeType().getLocalName());
        this.itemMap.put("primaryNodeTypeName", node.getPrimaryNodeType().getName());
        this.itemMap.put("primaryNodeTypeItemName", node.getPrimaryNodeType().getPrimaryItemName());
        this.itemMap.put("primaryNodeTypePrefix", node.getPrimaryNodeType().getPrefix());
        this.itemMap.put("type", node.getPrimaryNodeTypeName().split(":")[1]);
        this.itemMap.put("path", node.getPath());
        this.itemMap.put("identifier", node.getIdentifier());

        /* language data */
        this.itemMap.put("languages",this.langInformation.getLanguagesAsString());
        this.itemMap.put("uniqueLang",this.langInformation.isUniqueLang());
        this.itemMap.put("publishable", siteNode.getInactiveLiveLanguages().contains(language) ? "true" : "false");

        /* adding the lang information if exists. */
        if(this.langInformation.getNode() != null) {
            this.itemMap.put("created", dateFormat.format(this.langInformation.getNode().hasProperty(Constants.JCR_CREATED) ? this.langInformation.getNode().getProperty(Constants.JCR_CREATED).getDate().getTime() : node.getCreationDateAsDate()));
            this.itemMap.put("lastModified", dateFormat.format(this.langInformation.getNode().getProperty(Constants.JCR_LASTMODIFIED).getDate().getTime()));
            this.itemMap.put("lastModifiedBy", this.langInformation.getNode().getProperty(Constants.JCR_LASTMODIFIEDBY).getString());
            this.itemMap.put("published", this.langInformation.getNode().hasProperty("j:published") ? "true" : "false");
            this.itemMap.put("lock", this.langInformation.getNode().isLocked() ? "true" : "false");
            this.itemMap.put("language", this.langInformation.getNode().getProperty(Constants.JCR_LANGUAGE).getString());
            this.itemMap.put("path", this.langInformation.getNode().getPath());
            this.itemMap.put("langTitleOrText", this.langInformation.getNode().hasProperty("jcr:title") ? this.langInformation.getNode().getProperty("jcr:title").getString() : this.langInformation.getNode().hasProperty("text") ? this.langInformation.getNode().getProperty("text").getString() : "" );
        }

        /* adding the node lang information to map */
        this.listMap.add(itemMap);
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
        JSONArray jArray = new JSONArray();

        /* get all items from list */
        for (Map<String, String> itemMap : this.listMap) {
            jArray.put(new JSONObject(itemMap));
        }

        jsonObject.put("language", language);
        jsonObject.put("items", jArray);
        return jsonObject;
    }

    /**
     * getLanguageInformation
     * <p>returns the NodeLangInformation object if exists.</p>
     * @param parentNode
     * @return
     * @throws RepositoryException
     */
    private NodeLangInformation getLanguageInformation(JCRNodeWrapper parentNode) throws RepositoryException {
        NodeLangInformation langInformation = new NodeLangInformation();
        NodeIterator ni = parentNode.getI18Ns();

        while (ni.hasNext()){
            Node translationNode = ni.nextNode();
            langInformation.getLanguages().add(translationNode.getProperty("jcr:language").getString());

            if (translationNode.getProperty("jcr:language").getString().equalsIgnoreCase(this.language))
                langInformation.setNode(translationNode);
        }

        return langInformation;
    }

    /**
     * the NodeLangInformation class.
     */
    class NodeLangInformation{

        private List<String> languageList;
        private Node node;

        /* the constructor for the class. */
        public NodeLangInformation(){
            this.languageList =  new ArrayList<>();
        }

        /**
         * getLanguagesAsString
         * <p>returns all the languages for some node as string.</p>
         *
         * @return
         */
        public String getLanguagesAsString(){
            String langString = "";
            for (String lang: languageList) {
                if(!langString.equals("")) langString += ",";
                langString += lang;
            }
            return langString;

        }

        /**
         * isUniqueLang
         * <p> return true if is unique language, otherwise return false.</p>
         *
         * @return {@link String}
         */
        public String isUniqueLang(){
            return languageList.size() == 1 ? "true" : "false";
        }


        /**
         * Getter for property 'languages'.
         *
         * @return Value for property 'languages'.
         */
        public List<String> getLanguages() {
            return this.languageList;
        }

        /**
         * Setter for property 'languages'.
         *
         * @param languages Value to set for property 'languages'.
         */
        public void setLanguages(List<String> languages) {
            this.languageList = languages;
        }

        /**
         * Getter for property 'node'.
         *
         * @return Value for property 'node'.
         */
        public Node getNode() {
            return node;
        }

        /**
         * Setter for property 'node'.
         *
         * @param node Value to set for property 'node'.
         */
        public void setNode(Node node) {
            this.node = node;
        }
    }

}


