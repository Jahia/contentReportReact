import React, {useCallback, useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {Button} from '@jahia/moonstone';
import {useTranslation} from 'react-i18next';
import {USERS_GROUPS_FOLDER_CHILDREN_QUERY} from '../../graphql/usersGroupsQueries';
import styles from './UsersGroupsReport.module.scss';

const ROOT_FLOOR = '/sites';

const pathParts = path => path.split('/').filter(Boolean);
const pathUpTo = (parts, index) => '/' + parts.slice(0, index + 1).join('/');

const clampPath = path => {
    if (!path || path === '/' || path === '/sites') {
        return ROOT_FLOOR;
    }

    return path.startsWith(`${ROOT_FLOOR}/`) ? path : ROOT_FLOOR;
};

const UsersGroupsFolderPicker = ({executeQuery, initialPath, onSelect, onClose}) => {
    const {t} = useTranslation('contentReportReact');
    const [currentPath, setCurrentPath] = useState(clampPath(initialPath || ROOT_FLOOR));
    const [node, setNode] = useState(null);
    const [children, setChildren] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const navigate = useCallback(path => setCurrentPath(clampPath(path)), []);

    useEffect(() => {
        let mounted = true;
        const loadChildren = async () => {
            setLoading(true);
            setError(null);
            try {
                const data = await executeQuery(USERS_GROUPS_FOLDER_CHILDREN_QUERY, {path: currentPath});
                if (!mounted) {
                    return;
                }

                const nextNode = data?.jcr?.nodeByPath || null;
                setNode(nextNode);
                setChildren(nextNode?.children?.nodes || []);
            } catch (err) {
                if (mounted) {
                    setError(err);
                    setNode(null);
                    setChildren([]);
                }
            } finally {
                if (mounted) {
                    setLoading(false);
                }
            }
        };

        loadChildren();

        return () => {
            mounted = false;
        };
    }, [currentPath, executeQuery]);

    const parts = pathParts(currentPath);
    const sitesIndex = parts.indexOf('sites');

    return (
        <div className={styles.pickerOverlay} role="dialog" aria-modal="true" aria-label={t('usersGroups.picker.title')}>
            <div className={styles.pickerModal}>
                <div className={styles.pickerHeader}>
                    <span className={styles.pickerTitle}>{t('usersGroups.picker.title')}</span>
                    <button type="button" className={styles.pickerCloseButton} aria-label={t('usersGroups.picker.close')} onClick={onClose}>✕</button>
                </div>

                <div className={styles.pickerBreadcrumb}>
                    {parts.map((part, index) => {
                        const isActive = index === parts.length - 1;
                        const isAboveFloor = index < sitesIndex;
                        const crumbPath = pathUpTo(parts, index);

                        return (
                            <React.Fragment key={crumbPath}>
                                {index > 0 && <span className={styles.pickerCrumbSeparator}>/</span>}
                                {isAboveFloor ? (
                                    <span className={styles.pickerCrumbLocked}>{part}</span>
                                ) : (
                                    <button
                                        type="button"
                                        className={`${styles.pickerCrumb} ${isActive ? styles.pickerCrumbActive : ''}`}
                                        disabled={isActive}
                                        onClick={() => navigate(crumbPath)}
                                    >
                                        {part}
                                    </button>
                                )}
                            </React.Fragment>
                        );
                    })}
                </div>

                <div className={styles.pickerList}>
                    {loading && <div className={styles.pickerStatus}>{t('usersGroups.picker.loading')}</div>}
                    {error && <div className={styles.pickerStatus}>{t('usersGroups.picker.error')}</div>}
                    {!loading && !error && node === null && (
                        <div className={styles.pickerStatus}>{t('usersGroups.picker.notFound')}</div>
                    )}
                    {!loading && !error && node !== null && children.length === 0 && (
                        <div className={styles.pickerStatus}>{t('usersGroups.picker.empty')}</div>
                    )}
                    {children.map(child => (
                        <button
                            key={child.path}
                            type="button"
                            className={styles.pickerFolderButton}
                            onClick={() => navigate(child.path)}
                        >
                            <span className={styles.pickerFolderIcon}>📁</span>
                            <span>{child.name}</span>
                        </button>
                    ))}
                </div>

                <div className={styles.pickerFooter}>
                    <code className={styles.pickerSelected}>{currentPath}</code>
                    <div className={styles.pickerActions}>
                        <Button label={t('usersGroups.picker.select')} variant="primary" onClick={() => onSelect(currentPath)}/>
                        <Button label={t('usersGroups.picker.cancel')} variant="ghost" onClick={onClose}/>
                    </div>
                </div>
            </div>
        </div>
    );
};

UsersGroupsFolderPicker.propTypes = {
    executeQuery: PropTypes.func.isRequired,
    initialPath: PropTypes.string,
    onSelect: PropTypes.func.isRequired,
    onClose: PropTypes.func.isRequired
};

export default UsersGroupsFolderPicker;
