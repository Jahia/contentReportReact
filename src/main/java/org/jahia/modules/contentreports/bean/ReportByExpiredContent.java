/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.contentreports.bean;

import org.jahia.modules.contentreports.service.ExpiredConditionService;
import org.jahia.services.content.decorator.JCRSiteNode;

import java.util.Map;

/**
 * The ReportByExpiredContent class
 *
 * @author nonico
 */
public class ReportByExpiredContent extends AbstractDateConditionReport {

    /**
     * Constructor for ReportByExpiredContent
     * @param siteNode JCRSite node
     * @param searchPath path on where to perform the queries
     */
    public ReportByExpiredContent(JCRSiteNode siteNode, String searchPath) {
        super(siteNode, searchPath);
        this.conditionService = new ExpiredConditionService();
    }

    @Override
    protected String buildDateCondition(String nowFormatted) {
        return "condition.end < CAST('" + nowFormatted + "' AS DATE) ORDER BY parent.Name ASC";
    }

    @Override
    protected boolean matchesCondition(Map<String, String> conditions) {
        return conditions.size() == 1;
    }

    @Override
    protected String getDateKey() {
        return "expiresOn";
    }
}
