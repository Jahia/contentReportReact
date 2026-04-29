package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.contentreports.service.ReportUsersAndGroupsGenerator;
import org.jahia.modules.contentreports.service.ReportUsersAndGroupsService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.List;

@GraphQLName("ContentReportsUsersGroupsMutations")
@GraphQLDescription("Users and groups report mutations")
public class GqlContentReportsUsersGroupsMutations {

    private static final Logger LOGGER = LoggerFactory.getLogger(GqlContentReportsUsersGroupsMutations.class);
    private static final List<String> DEFAULT_USER_PROPERTIES = Arrays.asList("j:firstName", "j:lastName");

    @GraphQLField
    @GraphQLName("generate")
    @GraphQLDescription("Generates a CSV report of all users and their groups and stores it in JCR")
    public Boolean generate(
            @GraphQLName("csvRootPath") @GraphQLNonNull final String csvRootPath,
            @GraphQLName("userPropertiesToExport") final List<String> userPropertiesToExport) {
        final ReportUsersAndGroupsService service = BundleUtils.getOsgiService(ReportUsersAndGroupsService.class, null);
        if (service == null) {
            LOGGER.error("ReportUsersAndGroupsService is not available");
            return Boolean.FALSE;
        }

        final List<String> propertiesToExport = userPropertiesToExport != null ? userPropertiesToExport : DEFAULT_USER_PROPERTIES;
        return service.generate(csvRootPath, propertiesToExport);
    }

    @GraphQLField
    @GraphQLName("deleteReport")
    @GraphQLDescription("Deletes a generated users and groups report file from JCR by its JCR path")
    public Boolean deleteReport(@GraphQLName("path") @GraphQLNonNull final String path) {
        if (!ReportUsersAndGroupsGenerator.isReportFilePath(path)) {
            LOGGER.warn("Refusing to delete a non users-and-groups report path: {}", path);
            return Boolean.FALSE;
        }

        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession((JCRSessionWrapper session) -> {
                if (session.nodeExists(path)) {
                    session.getNode(path).remove();
                    session.save();
                }

                return null;
            });
            return Boolean.TRUE;
        } catch (RepositoryException e) {
            LOGGER.error("Error deleting users and groups report file at {}", path, e);
            return Boolean.FALSE;
        }
    }
}
