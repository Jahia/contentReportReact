import React, {useCallback, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import axios from 'axios';
import {Button, Loader, Paper, Typography} from '@jahia/moonstone';
import {useTranslation} from 'react-i18next';
import {
    USERS_GROUPS_DELETE_MUTATION,
    USERS_GROUPS_GENERATE_MUTATION,
    USERS_GROUPS_PROPERTIES_QUERY,
    USERS_GROUPS_STATUS_QUERY
} from '../../graphql/usersGroupsQueries';
import UsersGroupsFolderPicker from './UsersGroupsFolderPicker';
import styles from './UsersGroupsReport.module.scss';

const DEFAULT_CSV_ROOT_PATH = '/sites/systemsite/files';
const DEFAULT_SELECTED_PROPERTIES = ['j:firstName', 'j:lastName'];
const POLL_INTERVAL_MS = 2000;

const formatDate = isoString => {
    try {
        return new Intl.DateTimeFormat(undefined, {
            dateStyle: 'medium',
            timeStyle: 'short'
        }).format(new Date(isoString));
    } catch {
        return isoString;
    }
};

const fileNameFromPath = path => path.split('/').pop();

const buildDownloadUrl = downloadUrl => {
    const contextPath = window.contextJsParameters?.contextPath || '';
    return `${contextPath}${downloadUrl}`;
};

const UsersGroupsReport = ({graphqlEndpoint}) => {
    const {t} = useTranslation('contentReportReact');
    const [csvRootPath, setCsvRootPath] = useState(DEFAULT_CSV_ROOT_PATH);
    const [selectedProperties, setSelectedProperties] = useState(DEFAULT_SELECTED_PROPERTIES);
    const [availableProperties, setAvailableProperties] = useState([]);
    const [reportFiles, setReportFiles] = useState([]);
    const [generating, setGenerating] = useState(false);
    const [status, setStatus] = useState(null);
    const [loadingProperties, setLoadingProperties] = useState(false);
    const [loadingReports, setLoadingReports] = useState(false);
    const [pickerOpen, setPickerOpen] = useState(false);
    const [error, setError] = useState(null);

    const executeQuery = useCallback(async (query, variables = {}) => {
        const response = await axios.post(graphqlEndpoint, {query, variables}, {
            headers: {'Content-Type': 'application/json'},
            withCredentials: true
        });

        if (response.data?.errors?.length) {
            const message = response.data.errors.map(err => err.message).join('\n');
            throw new Error(message);
        }

        return response.data.data;
    }, [graphqlEndpoint]);

    const loadProperties = useCallback(async () => {
        setLoadingProperties(true);
        setError(null);
        try {
            const data = await executeQuery(USERS_GROUPS_PROPERTIES_QUERY);
            setAvailableProperties(data?.admin?.contentReportsUsersGroups?.userProperties || []);
        } catch (err) {
            setError(err);
        } finally {
            setLoadingProperties(false);
        }
    }, [executeQuery]);

    const loadStatus = useCallback(async () => {
        setLoadingReports(true);
        setError(null);
        try {
            const data = await executeQuery(USERS_GROUPS_STATUS_QUERY, {csvRootPath});
            setGenerating(data?.admin?.contentReportsUsersGroups?.isGenerating === true);
            setReportFiles(data?.admin?.contentReportsUsersGroups?.files || []);
        } catch (err) {
            setError(err);
        } finally {
            setLoadingReports(false);
        }
    }, [csvRootPath, executeQuery]);

    useEffect(() => {
        loadProperties();
    }, [loadProperties]);

    useEffect(() => {
        loadStatus();
    }, [loadStatus]);

    useEffect(() => {
        if (!generating) {
            return undefined;
        }

        const interval = window.setInterval(loadStatus, POLL_INTERVAL_MS);
        return () => window.clearInterval(interval);
    }, [generating, loadStatus]);

    const handleGenerate = async () => {
        setStatus(null);
        setError(null);
        setGenerating(true);
        try {
            const data = await executeQuery(USERS_GROUPS_GENERATE_MUTATION, {
                csvRootPath,
                userPropertiesToExport: selectedProperties
            });

            if (data?.admin?.contentReportsUsersGroups?.generate) {
                setStatus('success');
                await loadStatus();
            } else {
                setStatus('alreadyGenerating');
                await loadStatus();
            }
        } catch (err) {
            setError(err);
            setStatus('error');
        } finally {
            setGenerating(false);
        }
    };

    const handleDelete = async path => {
        setError(null);
        try {
            await executeQuery(USERS_GROUPS_DELETE_MUTATION, {path});
            await loadStatus();
        } catch (err) {
            setError(err);
        }
    };

    const handlePropertyToggle = name => {
        setSelectedProperties(prev => (
            prev.includes(name) ? prev.filter(property => property !== name) : [...prev, name]
        ));
    };

    const handleKeyDown = event => {
        if (event.key === 'Enter' && event.ctrlKey && csvRootPath.trim() && !generating) {
            handleGenerate();
        }
    };

    return (
        <Paper className={styles.container}>
            <div className={styles.header}>
                <Typography variant="heading" weight="bold" className={styles.title}>
                    {t('usersGroups.title')}
                </Typography>
                <Typography variant="body" className={styles.description}>
                    {t('usersGroups.description')}
                </Typography>
            </div>

            <div className={styles.form}>
                <div className={styles.fieldGroup}>
                    <label className={styles.label} htmlFor="content-reports-users-groups-root-path">
                        {t('usersGroups.csvRootPath')}
                    </label>
                    <div className={styles.inputRow}>
                        <input
                            type="text"
                            id="content-reports-users-groups-root-path"
                            className={styles.input}
                            value={csvRootPath}
                            onChange={event => {
                                setCsvRootPath(event.target.value);
                                setStatus(null);
                            }}
                            onKeyDown={handleKeyDown}
                        />
                        <Button label={t('usersGroups.browse')} variant="outlined" onClick={() => setPickerOpen(true)}/>
                    </div>
                    <span className={styles.hint}>{t('usersGroups.csvRootPathHint')}</span>
                </div>

                <div className={styles.fieldGroup}>
                    <span className={styles.label} id="content-reports-users-groups-properties-label">
                        {t('usersGroups.properties')}
                    </span>
                    <div className={styles.propertyControls}>
                        <Button label={t('usersGroups.selectAll')} variant="ghost" size="default" onClick={() => setSelectedProperties([...availableProperties])}/>
                        <Button label={t('usersGroups.clearAll')} variant="ghost" size="default" onClick={() => setSelectedProperties([])}/>
                    </div>
                    <div
                        id="content-reports-users-groups-properties"
                        role="group"
                        aria-labelledby="content-reports-users-groups-properties-label"
                        className={styles.propertyList}
                        tabIndex={-1}
                        onKeyDown={handleKeyDown}
                    >
                        {loadingProperties && <div className={styles.statusText}>{t('usersGroups.loadingProperties')}</div>}
                        {!loadingProperties && availableProperties.map(name => (
                            <label key={name} className={styles.propertyItem}>
                                <input
                                    type="checkbox"
                                    checked={selectedProperties.includes(name)}
                                    onChange={() => handlePropertyToggle(name)}
                                />
                                <span>{name}</span>
                            </label>
                        ))}
                    </div>
                    <span className={styles.hint}>{t('usersGroups.propertiesHint')}</span>
                </div>
            </div>

            {status === 'success' && <div className={styles.success}>{t('usersGroups.generateSuccess')}</div>}
            {status === 'alreadyGenerating' && <div className={styles.info}>{t('usersGroups.alreadyGenerating')}</div>}
            {status === 'error' && <div className={styles.error}>{t('usersGroups.generateError')}</div>}
            {error && <div className={styles.error}>{error.message}</div>}

            <div className={styles.actions}>
                {generating ? (
                    <div className={styles.loading}>
                        <Loader size="big"/>
                        <Typography className={styles.loadingText}>{t('usersGroups.generating')}</Typography>
                    </div>
                ) : (
                    <Button
                        label={t('usersGroups.generate')}
                        variant="primary"
                        isDisabled={!csvRootPath.trim()}
                        onClick={handleGenerate}
                    />
                )}
            </div>

            <div className={styles.reportsSection}>
                <div className={styles.reportsHeader}>
                    <Typography variant="heading" weight="bold" className={styles.reportsTitle}>
                        {t('usersGroups.reportsTitle')}
                    </Typography>
                    <Button label={t('usersGroups.refresh')} variant="ghost" size="default" isDisabled={loadingReports} onClick={loadStatus}/>
                </div>
                {reportFiles.length > 0 ? (
                    <table className={styles.table}>
                        <thead>
                            <tr>
                                <th>{t('usersGroups.colDate')}</th>
                                <th>{t('usersGroups.colFile')}</th>
                                <th>{t('usersGroups.colActions')}</th>
                            </tr>
                        </thead>
                        <tbody>
                            {reportFiles.map(file => (
                                <tr key={file.path}>
                                    <td>{formatDate(file.createdAt)}</td>
                                    <td>
                                        <a
                                            href={buildDownloadUrl(file.downloadUrl)}
                                            download={fileNameFromPath(file.path)}
                                            className={styles.downloadLink}
                                        >
                                            {fileNameFromPath(file.path)}
                                        </a>
                                    </td>
                                    <td>
                                        <Button label={t('usersGroups.delete')} variant="ghost" size="default" onClick={() => handleDelete(file.path)}/>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                ) : (
                    <div className={styles.statusText}>
                        {loadingReports ? t('usersGroups.loadingReports') : t('usersGroups.noReports')}
                    </div>
                )}
            </div>

            {pickerOpen && (
                <UsersGroupsFolderPicker
                    executeQuery={executeQuery}
                    initialPath={csvRootPath || DEFAULT_CSV_ROOT_PATH}
                    onSelect={path => {
                        setCsvRootPath(path);
                        setStatus(null);
                        setPickerOpen(false);
                    }}
                    onClose={() => setPickerOpen(false)}
                />
            )}
        </Paper>
    );
};

UsersGroupsReport.propTypes = {
    graphqlEndpoint: PropTypes.string.isRequired
};

export default UsersGroupsReport;
