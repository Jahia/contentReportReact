package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminMutation;

@GraphQLTypeExtension(GqlAdminMutation.class)
@GraphQLDescription("Content report GraphQL mutation entry point for admin tooling")
public class ContentReportsMutationExtension {

    @GraphQLField
    @GraphQLName("contentReportsUsersGroups")
    @GraphQLDescription("Access users and groups report mutations")
    public static GqlContentReportsUsersGroupsMutations contentReportsUsersGroups() {
        return new GqlContentReportsUsersGroupsMutations();
    }
}
