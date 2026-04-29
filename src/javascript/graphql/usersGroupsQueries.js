export const USERS_GROUPS_FOLDER_CHILDREN_QUERY = `
    query ContentReportsUsersGroupsFolderChildren($path: String!) {
        jcr {
            nodeByPath(path: $path) {
                path
                name
                children(typesFilter: {types: ["jnt:folder", "jnt:virtualsite"], multi: ANY}) {
                    nodes {
                        path
                        name
                    }
                }
            }
        }
    }
`;

export const USERS_GROUPS_PROPERTIES_QUERY = `
    query ContentReportsUsersGroupsProperties {
        admin {
            contentReportsUsersGroups {
                userProperties
            }
        }
    }
`;

export const USERS_GROUPS_STATUS_QUERY = `
    query ContentReportsUsersGroupsStatus($csvRootPath: String!) {
        admin {
            contentReportsUsersGroups {
                isGenerating
                files(csvRootPath: $csvRootPath) {
                    path
                    downloadUrl
                    createdAt
                }
            }
        }
    }
`;

export const USERS_GROUPS_GENERATE_MUTATION = `
    mutation ContentReportsUsersGroupsGenerate($csvRootPath: String!, $userPropertiesToExport: [String]) {
        admin {
            contentReportsUsersGroups {
                generate(csvRootPath: $csvRootPath, userPropertiesToExport: $userPropertiesToExport)
            }
        }
    }
`;

export const USERS_GROUPS_DELETE_MUTATION = `
    mutation ContentReportsUsersGroupsDelete($path: String!) {
        admin {
            contentReportsUsersGroups {
                deleteReport(path: $path)
            }
        }
    }
`;
