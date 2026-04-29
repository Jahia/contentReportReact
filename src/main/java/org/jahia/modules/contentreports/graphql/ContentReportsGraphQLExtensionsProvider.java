package org.jahia.modules.contentreports.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.Collection;

/**
 * Exposes this bundle's @GraphQLTypeExtension classes to the DX GraphQL provider.
 */
@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class ContentReportsGraphQLExtensionsProvider implements DXGraphQLExtensionsProvider {

    @Override
    public Collection<Class<?>> getExtensions() {
        return Arrays.asList(
                ContentReportsQueryExtension.class,
                ContentReportsMutationExtension.class);
    }
}
