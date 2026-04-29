package org.jahia.modules.contentreports.service;

import org.apache.commons.lang3.StringUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.scheduler.BackgroundJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReportUsersAndGroupsBackgroundJob extends BackgroundJob {

    @Override
    public void executeJahiaJob(JobExecutionContext jobExecutionContext) {
        final JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        final String csvRootPath = jobDataMap.getString("csvRootPath");
        final List<String> userPropertiesToExport = splitProperties(jobDataMap.getString("userPropertiesToExport"));
        final ReportUsersAndGroupsService service = BundleUtils.getOsgiService(ReportUsersAndGroupsService.class, null);
        if (service != null) {
            service.generate(csvRootPath, userPropertiesToExport);
        } else {
            ReportUsersAndGroupsGenerator.generate(csvRootPath, userPropertiesToExport);
        }
    }

    private List<String> splitProperties(String userPropertiesToExport) {
        if (StringUtils.isBlank(userPropertiesToExport)) {
            return Collections.emptyList();
        }

        return Arrays.asList(userPropertiesToExport.split(","));
    }
}
