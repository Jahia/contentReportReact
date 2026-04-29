package org.jahia.modules.contentreports.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jahia.api.Constants;
import org.jahia.modules.contentreports.bean.ReportUsersAndGroupsUserInfo;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.QueryManagerWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.query.QueryWrapper;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ReportUsersAndGroupsGenerator {

    public static final String DEFAULT_CSV_ROOT_PATH = "/sites/systemsite/files";
    public static final String REPORT_FOLDER_NAME = "report-users-and-groups";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportUsersAndGroupsGenerator.class);
    private static final String HUMAN_READABLE_FORMAT = "d' days 'H' hours 'm' minutes 's' seconds'";
    private static final String FILE_NAME = "report-users-and-groups";
    private static final String FILE_EXT = ".csv";
    private static final Path TMP_PATH = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"));
    private static final long LIMIT = 100L;
    private static final Set<String> SYSTEM_GROUPS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "guest",
            "privileged",
            "site-privileged",
            "site-users",
            "users"
    )));

    // Sensitive properties that must never be exported, regardless of caller input
    private static final Set<String> BLOCKED_PROPERTIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "j:password",
            "rep:password"
    )));

    private ReportUsersAndGroupsGenerator() {
    }

    public static void generate(String csvRootPath, List<String> userPropertiesToExport) {
        final String rootPath = normalizeRootPath(csvRootPath);
        final List<String> propertiesToExport = normalizeProperties(userPropertiesToExport);

        try {
            final long start = System.currentTimeMillis();
            LOGGER.info("Starting users and groups report generation");

            final List<ReportUsersAndGroupsUserInfo> usersInfos = new ArrayList<>();
            usersInfos.addAll(JCRTemplate.getInstance().doExecuteWithSystemSession(
                    (JCRSessionWrapper session) -> computeUsersAndGroups(session, "global", "/users", propertiesToExport)));

            for (String siteName : JahiaSitesService.getInstance().getSitesNames()) {
                final String usersPath = String.format("/sites/%s/users", siteName);
                usersInfos.addAll(JCRTemplate.getInstance().doExecuteWithSystemSession(
                        (JCRSessionWrapper session) -> computeUsersAndGroups(session, siteName, usersPath, propertiesToExport)));
            }

            writeCsvData(rootPath, usersInfos, propertiesToExport);

            final long end = System.currentTimeMillis();
            LOGGER.info("Finished users and groups report generation in {}",
                    DurationFormatUtils.formatDuration(end - start, HUMAN_READABLE_FORMAT, true));
        } catch (RepositoryException e) {
            LOGGER.error("Unable to retrieve users and groups", e);
        }
    }

    public static String normalizeRootPath(String csvRootPath) {
        String rootPath = StringUtils.defaultIfBlank(csvRootPath, DEFAULT_CSV_ROOT_PATH).trim();
        while (rootPath.length() > 1 && rootPath.endsWith("/")) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }
        // Reject paths outside /sites/ and any path-traversal sequences
        if (!rootPath.startsWith("/sites/") || rootPath.contains("/.")) {
            LOGGER.warn("Rejecting unsafe csvRootPath '{}', using default", rootPath);
            return DEFAULT_CSV_ROOT_PATH;
        }
        return rootPath;
    }

    public static boolean isReportFilePath(String path) {
        return StringUtils.isNotBlank(path)
                && !path.contains("/..")
                && path.contains("/" + REPORT_FOLDER_NAME + "/");
    }

    private static List<String> normalizeProperties(List<String> userPropertiesToExport) {
        if (userPropertiesToExport == null || userPropertiesToExport.isEmpty()) {
            return Collections.emptyList();
        }

        final LinkedHashSet<String> uniqueProperties = new LinkedHashSet<>();
        for (String property : userPropertiesToExport) {
            if (StringUtils.isNotBlank(property) && !BLOCKED_PROPERTIES.contains(property.trim())) {
                uniqueProperties.add(property.trim());
            }
        }

        return new ArrayList<>(uniqueProperties);
    }

    private static void writeCsvData(String rootPath, List<ReportUsersAndGroupsUserInfo> usersInfos, List<String> userPropertiesToExport) {
        Path csvPath = null;
        try {
            csvPath = Files.createTempFile(TMP_PATH, FILE_NAME, FILE_EXT);
            final Date now = new Date();
            final String storageFolder = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss").format(now);
            final String dateStamp = new SimpleDateFormat("yyyyMMdd-HHmm").format(now);
            final String csvFileName = FILE_NAME + "-" + dateStamp + FILE_EXT;

            try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
                final List<String> headers = new ArrayList<>();
                headers.add("site");
                headers.add("user name");
                headers.addAll(userPropertiesToExport);
                headers.add("groups");
                writeCsvRow(writer, headers);

                for (ReportUsersAndGroupsUserInfo userInfo : usersInfos) {
                    final List<String> userValues = new ArrayList<>();
                    userValues.add(userInfo.getSite());
                    userValues.add(userInfo.getName());
                    for (String userProperty : userPropertiesToExport) {
                        userValues.add(userInfo.containsPropertyKey(userProperty) ? userInfo.getProperty(userProperty) : "");
                    }

                    userValues.add(userInfo.getGroups().toString());
                    writeCsvRow(writer, userValues);
                }
            }

            try (InputStream csvInputStream = Files.newInputStream(csvPath)) {
                JCRTemplate.getInstance().doExecuteWithSystemSession((JCRSessionWrapper session) -> {
                    final JCRNodeWrapper folderNode = mkdirs(session, rootPath + "/" + REPORT_FOLDER_NAME + "/" + storageFolder);
                    folderNode.uploadFile(csvFileName, csvInputStream, "text/csv");
                    folderNode.saveSession();
                    return null;
                });
            }
        } catch (IOException | RepositoryException e) {
            LOGGER.error("Unable to create users and groups CSV file", e);
        } finally {
            if (csvPath != null) {
                try {
                    Files.deleteIfExists(csvPath);
                } catch (IOException e) {
                    LOGGER.error("Unable to delete temporary users and groups CSV file", e);
                }
            }
        }
    }

    private static List<ReportUsersAndGroupsUserInfo> computeUsersAndGroups(JCRSessionWrapper session,
                                                                            String site,
                                                                            String jcrPath,
                                                                            List<String> userPropertiesToExport)
            throws RepositoryException {
        final List<ReportUsersAndGroupsUserInfo> users = new ArrayList<>();
        if (!session.nodeExists(jcrPath)) {
            return users;
        }

        boolean hasNextResults = true;
        long offsetMultiplier = 0L;
        while (hasNextResults) {
            session.refresh(false);
            final QueryManagerWrapper manager = session.getWorkspace().getQueryManager();
            final String queryStmt = String.format("SELECT * FROM [%s] AS users WHERE ISDESCENDANTNODE(users, '%s')",
                    "jnt:user",
                    JCRContentUtils.sqlEncode(jcrPath));
            final QueryWrapper query = manager.createQuery(queryStmt, Query.JCR_SQL2);
            query.setOffset(offsetMultiplier * LIMIT);
            query.setLimit(LIMIT);
            final JCRNodeIteratorWrapper nodeIterator = query.execute().getNodes();
            long resultCount = 0L;

            while (nodeIterator.hasNext()) {
                resultCount++;
                final JCRUserNode userNode = (JCRUserNode) nodeIterator.next();
                LOGGER.debug("Adding user {} to users and groups report", userNode.getPath());
                final ReportUsersAndGroupsUserInfo userInfo = new ReportUsersAndGroupsUserInfo(site, userNode.getName());

                for (String userProperty : userPropertiesToExport) {
                    if (userNode.hasProperty(userProperty)) {
                        userInfo.addProperty(userProperty, userNode.getPropertyAsString(userProperty));
                    }
                }

                for (String groupPath : JahiaGroupManagerService.getInstance().getMembershipByPath(userNode.getPath())) {
                    final String[] parts = groupPath.split("/");
                    final String group = parts[parts.length - 1];
                    if (!SYSTEM_GROUPS.contains(group)) {
                        userInfo.addGroup(group);
                    }
                }

                users.add(userInfo);
            }

            hasNextResults = resultCount == LIMIT;
            offsetMultiplier++;
        }

        session.refresh(false);
        return users;
    }

    private static JCRNodeWrapper mkdirs(JCRSessionWrapper session, String path) throws RepositoryException {
        JCRNodeWrapper folderNode = session.getRootNode();
        for (String folder : path.split("/")) {
            if (StringUtils.isNotBlank(folder)) {
                folderNode = folderNode.hasNode(folder) ? folderNode.getNode(folder) : folderNode.addNode(folder, "jnt:folder");
            }
        }

        return folderNode;
    }

    private static void writeCsvRow(BufferedWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(",");
            }

            writer.write(escapeCsv(values.get(i)));
        }

        writer.write("\n");
    }

    private static String escapeCsv(String value) {
        String safeValue = StringUtils.defaultString(value);
        // Prevent CSV formula injection: prefix values starting with formula-initiating chars
        if (!safeValue.isEmpty() && "=+-@".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = " " + safeValue;
        }
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }
}
