import {registry} from '@jahia/ui-extender';
import React, {Suspense} from 'react';
import AdminPanel from './AdminPanel';
import {DEFAULT_ROUTE} from './AdminPanel.constants';

// Helper to create a render function for a specific report
const createReportRender = reportId => {
    return v => {
        // Create a mock match object that simulates routing to the specific report
        const mockMatch = {
            ...v.match,
            params: {
                ...v.match?.params,
                reportId
            }
        };

        return (
            <Suspense fallback="loading...">
                <AdminPanel match={mockMatch} initialReportId={reportId}/>
            </Suspense>
        );
    };
};

export const registerJContentRoutes = () => {
    // Load i18n namespace
    window.jahia.i18n.loadNamespaces('contentReportReact');

    // Main content reports entry in jContent left nav (not selectable, acts as category)
    registry.add('adminRoute', 'jcontent-contentReports', {
        targets: ['jcontent:1'],
        isSelectable: true,
        icon: window.jahia.moonstone.toIconComponent('Pie'),
        label: 'contentReportReact:label',
        path: `${DEFAULT_ROUTE}*`, // Catch everything and let the app handle routing logic
        defaultPath: DEFAULT_ROUTE,
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('overview')

    });

    // === CONTENT REPORTS ===

    // By Author and Date
    registry.add('adminRoute', 'contentReportReact-byAuthorAndDate', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.byAuthorAndDate',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('20')
    });

    // WIP Content
    registry.add('adminRoute', 'contentReportReact-wipContent', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.wipContent',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('22')
    });

    // Content Waiting Publication
    registry.add('adminRoute', 'contentReportReact-contentWaitingPublication', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.contentWaitingPublication',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('16')
    });

    // Marked For Deletion
    registry.add('adminRoute', 'contentReportReact-markedForDeletion', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.markedForDeletion',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('24')
    });

    // References section label (not selectable)
    registry.add('adminRoute', 'contentReportReact-reference-entry', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.references',
        requireModuleInstalledOnSite: 'contentReportReact'
    });

    // References
    registry.add('adminRoute', 'contentReportReact-references', {
        targets: ['jcontent-contentReportReact-reference-entry'],
        isSelectable: true,
        label: 'contentReportReact:menu.references',
        requireModuleInstalledOnSite: 'contentReportReact',
        render: createReportRender('23')
    });
    // Unused assets
    registry.add('adminRoute', 'contentReportReact-unreferencedAssets', {
        targets: ['jcontent-contentReportReact-reference-entry'],
        isSelectable: true,
        label: 'contentReportReact:menu.unusedAssets',
        requireModuleInstalledOnSite: 'contentReportReact',
        render: createReportRender('28')
    });
    // === LANGUAGE CATEGORY ===

    // Language category (not selectable)
    registry.add('adminRoute', 'contentReportReact-language', {
        isSelectable: false,
        targets: ['jcontent-jcontent-contentReports'],
        label: 'contentReportReact:categories.languages',
        requireModuleInstalledOnSite: 'contentReportReact'
    });

    // Pages Without Title
    registry.add('adminRoute', 'contentReportReact-language-pagesWithoutTitle', {
        targets: ['jcontent-contentReportReact-language'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesWithoutTitle',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('10')
    });

    // Pages Untranslated
    registry.add('adminRoute', 'contentReportReact-language-pagesUntranslated', {
        targets: ['jcontent-contentReportReact-language'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesUntranslated',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('21')
    });

    // === VISIBILITY CONDITIONS CATEGORY ===

    // Visibility Conditions category (not selectable)
    registry.add('adminRoute', 'contentReportReact-visibility-conditions', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.visibility',
        requireModuleInstalledOnSite: 'contentReportReact'
    });

    // Live Contents
    registry.add('adminRoute', 'contentReportReact-visibility-conditions-liveContents', {
        targets: ['jcontent-contentReportReact-visibility-conditions'],
        isSelectable: true,
        label: 'contentReportReact:menu.liveContents',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('25')
    });

    // Expired Contents
    registry.add('adminRoute', 'contentReportReact-visibility-conditions-expiredContents', {
        targets: ['jcontent-contentReportReact-visibility-conditions'],
        isSelectable: true,
        label: 'contentReportReact:menu.expiredContents',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('26')
    });

    // Future Contents
    registry.add('adminRoute', 'contentReportReact-visibility-conditions-futureContents', {
        targets: ['jcontent-contentReportReact-visibility-conditions'],
        isSelectable: true,
        label: 'contentReportReact:menu.futureContents',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('27')
    });

    // === METADATA CATEGORY ===

    // Metadata category (not selectable)
    registry.add('adminRoute', 'contentReportReact-metadata', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.metadata',
        requireModuleInstalledOnSite: 'contentReportReact'
    });

    // Pages Without Keywords
    registry.add('adminRoute', 'contentReportReact-metadata-pagesWithoutKeywords', {
        targets: ['jcontent-contentReportReact-metadata'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesWithoutKeywords',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('11')
    });

    // Pages Without Description
    registry.add('adminRoute', 'contentReportReact-metadata-pagesWithoutDescription', {
        targets: ['jcontent-contentReportReact-metadata'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesWithoutDescription',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('12')
    });

    // === USERS & GROUPS CATEGORY ===

    registry.add('adminRoute', 'contentReportReact-users-groups', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.usersGroups',
        requireModuleInstalledOnSite: 'contentReportReact'
    });

    registry.add('adminRoute', 'contentReportReact-users-groups-report', {
        targets: ['jcontent-contentReportReact-users-groups'],
        isSelectable: true,
        label: 'contentReportReact:menu.usersGroups',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('usersGroups')
    });

    // === SYSTEM CATEGORY ===

    // System category (not selectable)
    registry.add('adminRoute', 'contentReportReact-system', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.system',
        requireModuleInstalledOnSite: 'contentReportReact'
    });

    // Locked Content
    registry.add('adminRoute', 'contentReportReact-system-lockedContent', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.lockedContent',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('15')
    });

    // Custom Cache Content
    registry.add('adminRoute', 'contentReportReact-system-customCacheContent', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.customCacheContent',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('18')
    });

    // ACL Inheritance Break
    registry.add('adminRoute', 'contentReportReact-system-aclInheritanceBreak', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.aclInheritanceBreak',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('19')
    });

    // System Overview
    registry.add('adminRoute', 'contentReportReact-system-overview', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.overview',
        requireModuleInstalledOnSite: 'contentReportReact',

        render: createReportRender('overview')
    });

    console.debug('%c contentReportReact jContent routes registered', 'color: #3c8cba');
};
