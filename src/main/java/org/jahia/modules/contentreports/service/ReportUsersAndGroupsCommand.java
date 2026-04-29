package org.jahia.modules.contentreports.service;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.osgi.BundleUtils;

import java.util.ArrayList;
import java.util.List;

@Command(scope = "content-reports", name = "report-users-and-groups", description = "Create a CSV report with all users and their groups for all websites")
@Service
public class ReportUsersAndGroupsCommand implements Action {

    @Option(name = "-p", aliases = "--csvRootPath", description = "Root JCR path where CSV files are stored")
    private String csvRootPath = ReportUsersAndGroupsGenerator.DEFAULT_CSV_ROOT_PATH;

    @Argument(index = 0, name = "userPropertiesToExport", description = "User properties to export", required = false, multiValued = true)
    private List<String> userPropertiesToExport = new ArrayList<>();

    @Override
    public Object execute() {
        final ReportUsersAndGroupsService service = BundleUtils.getOsgiService(ReportUsersAndGroupsService.class, null);
        if (service != null) {
            service.generate(csvRootPath, userPropertiesToExport);
        } else {
            ReportUsersAndGroupsGenerator.generate(csvRootPath, userPropertiesToExport);
        }

        return null;
    }
}
