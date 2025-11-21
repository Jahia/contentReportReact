import React, {useState, useMemo, useCallback} from 'react';
import PropTypes from 'prop-types';
import {useTranslation} from 'react-i18next';
import {Typography, Table, TableHead, TableBody, TableRow, TableHeadCell, TableBodyCell, TablePagination, Button, Tooltip} from '@jahia/moonstone';
import {Download, OpenInNew, File, FileImage, FileVideo, FileSound, FilePdf, FileZip, FileText} from '@jahia/moonstone/dist/icons';
import styles from './ReportResultsTable.module.scss';

const formatDate = dateString => {
    if (!dateString) {
        return '-';
    }

    try {
        const date = new Date(dateString);
        return new Intl.DateTimeFormat('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    } catch {
        return dateString;
    }
};

const renderBooleanValue = value => {
    const isTrue = value === true || value === 'true' || value === 'True' || value === '1';
    const isFalse = value === false || value === 'false' || value === 'False' || value === '0';

    if (isTrue) {
        return <span className={styles.booleanTrue}>✓</span>;
    }

    if (isFalse) {
        return <span className={styles.booleanFalse}>✗</span>;
    }

    return '-';
};

// Helper function to export data to CSV
const exportDataToCSV = (data, columns, useCustomColumns) => {
    const headers = useCustomColumns ?
        columns.map(col => col.key) :
        ['Title', 'Path', 'Type', 'Created', 'Modified', 'Published', 'Locked'];

    const csvContent = [
        headers.join(','),
        ...data.map(row => row.map(cell => {
            const cellStr = String(cell || '');
            return cellStr.includes(',') || cellStr.includes('"') ?
                `"${cellStr.replace(/"/g, '""')}"` :
                cellStr;
        }).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `report_${new Date().getTime()}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
};

// Helper function to export data to JSON
const exportDataToJSON = data => {
    const jsonContent = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonContent], {type: 'application/json;charset=utf-8;'});
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `report_${new Date().getTime()}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
};

// Helper function to build jContent edit URL
const buildJContentUrl = (path, siteKey, language) => {
    const baseUrl = window.contextJsParameters?.contextPath || '';
    // Remove /sites/{siteKey} prefix from path if present
    let cleanPath = path.replace(`/sites/${siteKey}`, '');
    // Remove leading slash for jContent URL format
    cleanPath = cleanPath.startsWith('/') ? cleanPath.substring(1) : cleanPath;
    // Build jContent URL: /jahia/jcontent/{siteKey}/{language}/pages/{path}
    return `${baseUrl}/jahia/jcontent/${siteKey}/${language || 'en'}/pages/${cleanPath}`;
};

const buildMediaFolderUrl = (path, siteKey, language) => {
    if (!path || !siteKey) {
        return null;
    }

    const baseUrl = window.contextJsParameters?.contextPath || '';
    const mediaRoot = `/sites/${siteKey}/files`;
    if (!path.startsWith(mediaRoot)) {
        return null;
    }

    const lastSlash = path.lastIndexOf('/');
    let parentPath = lastSlash > mediaRoot.length ? path.substring(0, lastSlash) : mediaRoot;
    let relativePath = parentPath.substring(mediaRoot.length);
    relativePath = relativePath.replace(/^\/+/, '');
    const suffix = relativePath ? `/${relativePath}` : '';

    return `${baseUrl}/jahia/jcontent/${siteKey}/${language || 'en'}/media/files${suffix}`;
};

const getMimeIconComponent = mimeType => {
    if (!mimeType) {
        return File;
    }

    const lower = mimeType.toLowerCase();
    if (lower.startsWith('image/')) {
        return FileImage;
    }

    if (lower.startsWith('video/')) {
        return FileVideo;
    }

    if (lower.startsWith('audio/')) {
        return FileSound;
    }

    if (lower === 'application/pdf') {
        return FilePdf;
    }

    if (lower.includes('zip') || lower.includes('compressed')) {
        return FileZip;
    }

    if (lower.startsWith('text/')) {
        return FileText;
    }

    return File;
};

const isImageMimeType = mimeType => {
    return typeof mimeType === 'string' && mimeType.toLowerCase().startsWith('image/');
};

const buildAssetPreviewUrl = (path, lastModified) => {
    const baseUrl = window.contextJsParameters?.contextPath || '';
    if (!path) {
        return '';
    }

    let url = `${baseUrl}/files/default${path}`;

    if (lastModified) {
        const timestamp = Date.parse(lastModified);
        if (!Number.isNaN(timestamp)) {
            url += `${url.includes('?') ? '&' : '?'}v=${timestamp}`;
        }
    }

    return url;
};

// Component to render table header cells
const TableHeaders = ({useCustomColumns, columns, sortColumn, sortDirection, handleSort, t, siteLanguages}) => {
    if (useCustomColumns) {
        return columns.map((column, index) => {
            // For language columns in i18n reports, display the language code directly
            const headerLabel = column.isLanguage && siteLanguages && siteLanguages[column.languageIndex - 1] ?
                siteLanguages[column.languageIndex - 1] :
                t(column.labelKey);

            const headerClassName = column.sortable ?
                `${styles.sortableHeader} ${column.type === 'icon' ? styles.mimeIconHeader : ''}`.trim() :
                (column.type === 'icon' ? styles.mimeIconHeader : undefined);

            return (
                <TableHeadCell
                    key={column.key}
                    className={headerClassName}
                    onClick={column.sortable ? () => handleSort(index) : undefined}
                >
                    <span className={styles.headerContent}>
                        {headerLabel}
                        {column.sortable && sortColumn === index && (
                            <span className={styles.sortIcon}>{sortDirection === 'asc' ? '▲' : '▼'}</span>
                        )}
                    </span>
                </TableHeadCell>
            );
        });
    }

    // Default headers for backward compatibility
    const defaultHeaders = [
        {label: 'Title', index: 0},
        {label: 'Path', index: 1},
        {label: 'Type', index: 2},
        {label: 'Created', index: 3},
        {label: 'Modified', index: 4},
        {label: 'Published', index: 5},
        {label: 'Lock', index: 6}
    ];

    return defaultHeaders.map(({label, index}) => (
        <TableHeadCell key={label} className={styles.sortableHeader} onClick={() => handleSort(index)}>
            <span className={styles.headerContent}>
                {label}
                {sortColumn === index && (
                    <span className={styles.sortIcon}>{sortDirection === 'asc' ? '▲' : '▼'}</span>
                )}
            </span>
        </TableHeadCell>
    ));
};

TableHeaders.propTypes = {
    useCustomColumns: PropTypes.bool.isRequired,
    columns: PropTypes.array,
    sortColumn: PropTypes.number,
    sortDirection: PropTypes.string.isRequired,
    handleSort: PropTypes.func.isRequired,
    t: PropTypes.func.isRequired,
    siteLanguages: PropTypes.arrayOf(PropTypes.string)
};

const ReportResultsTable = ({data, siteKey, language, columns, reportId, reportType}) => {
    // Hooks must be at the top before any conditionals
    const {t} = useTranslation('contentReportReact');
    const [currentPage, setCurrentPage] = useState(1);
    const [rowsPerPage, setRowsPerPage] = useState(50);
    const [sortColumn, setSortColumn] = useState(null);
    const [sortDirection, setSortDirection] = useState('asc');
    const [imagePreview, setImagePreview] = useState(null);

    console.log('ReportResultsTable - data:', data);
    console.log('ReportResultsTable - siteKey:', siteKey);
    console.log('ReportResultsTable - language:', language);
    console.log('ReportResultsTable - columns:', columns);
    console.log('ReportResultsTable - reportId:', reportId);
    console.log('ReportResultsTable - reportType:', reportType);

    // For i18n reports, detect site languages and generate dynamic language columns
    const siteLanguages = useMemo(() => {
        if (reportType === 'i18n' && data?.siteLanguages) {
            return data.siteLanguages;
        }

        return null;
    }, [reportType, data]);

    // For i18n reports, generate dynamic language columns from the first data row
    const dynamicColumns = useMemo(() => {
        if (reportType === 'i18n' && data?.data && data.data.length > 0) {
            const firstRow = data.data[0];
            const cols = [
                {key: 'path', labelKey: 'fields.path', sortable: true, type: 'link'}
            ];

            // Remaining columns are language codes (en, fr, etc.)
            for (let i = 1; i < firstRow.length; i++) {
                // Generate language column - the header will be the language code itself
                cols.push({
                    key: `lang_${i}`,
                    labelKey: `lang_${i}`, // We'll handle this specially in rendering
                    sortable: true,
                    isLanguage: true,
                    languageIndex: i
                });
            }

            return cols;
        }

        return null;
    }, [reportType, data]);

    // Determine if we should use custom columns
    const useCustomColumns = (columns && columns.length > 0) || (reportType === 'i18n' && dynamicColumns);
    const effectiveColumns = dynamicColumns || columns;

    // Helper function to render cell value based on column type
    const renderCellValue = useCallback((value, columnType, rowData, columnKey) => {
        const normalizedKey = columnKey ? columnKey.toLowerCase() : '';
        const isPathValue = normalizedKey.includes('path');
        const pathLabel = value !== null && value !== undefined ? String(value) : '';

        if (columnType === 'date') {
            return formatDate(value);
        }

        if (columnType === 'icon') {
            const Icon = getMimeIconComponent(value);
            return (
                <span className={styles.mimeIconWrapper} title={value || t('reports.unusedAssets.columns.mimeType')}>
                    <Icon size="default"/>
                </span>
            );
        }

        if (columnType === 'boolean') {
            return renderBooleanValue(value);
        }

        if (columnType === 'html') {
            // Render HTML content (interprets <br/> and other HTML tags)
            // eslint-disable-next-line react/no-danger
            return value ? <span dangerouslySetInnerHTML={{__html: value}}/> : '-';
        }

        if (columnType === 'link' && siteKey && language) {
            if (reportId === '28') {
                const targetPath = columnKey === 'path' ? value : (value || rowData[4]);
                const mediaUrl = buildMediaFolderUrl(targetPath, siteKey, language);
                if (mediaUrl) {
                    return (
                        <a href={mediaUrl} target="_blank" rel="noopener noreferrer" className={styles.pathLink} title={targetPath}>
                            {value}
                            <OpenInNew size="small" style={{marginLeft: '4px', verticalAlign: 'middle'}}/>
                        </a>
                    );
                }
            }

            // For path columns in i18n reports, show the path as a link
            if (columnKey === 'path' && value) {
                const jcontentUrl = buildJContentUrl(value, siteKey, language);
                return (
                    <a
                        href={jcontentUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className={styles.pathLink}
                        title={value}
                    >
                        {value}
                        <OpenInNew size="small" style={{marginLeft: '4px', verticalAlign: 'middle'}}/>
                    </a>
                );
            }

            // For other link columns (like Page), use the value directly if it's a path, or fall back to index 4
            const path = value || rowData[4];
            if (path) {
                const jcontentUrl = buildJContentUrl(path, siteKey, language);
                return (
                    <a href={jcontentUrl} target="_blank" rel="noopener noreferrer" className={styles.linkWithIcon}>
                        {t('result.viewContent')}
                        <OpenInNew size="small" style={{marginLeft: '4px', verticalAlign: 'middle'}}/>
                    </a>
                );
            }

            return '-';
        }

        if (isPathValue && value) {
            return (
                <Tooltip label={pathLabel}>
                    <span className={styles.pathText}>{value}</span>
                </Tooltip>
            );
        }

        return value || '-';
    }, [siteKey, language, t, reportId]);

    // Handle column sorting
    const handleSort = columnIndex => {
        if (sortColumn === columnIndex) {
            // Toggle direction if clicking same column
            setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
        } else {
            // New column, default to ascending
            setSortColumn(columnIndex);
            setSortDirection('asc');
        }

        // Reset to first page when sorting
        setCurrentPage(1);
    };

    // Helper function to compare values for sorting
    const compareValues = useCallback((aVal, bVal, columnIndex) => {
        // Handle null/undefined values
        if (aVal === null || aVal === undefined) {
            return 1;
        }

        if (bVal === null || bVal === undefined) {
            return -1;
        }

        // For dates (columns 3, 4: created, modified)
        if (columnIndex === 3 || columnIndex === 4) {
            const aDate = new Date(aVal);
            const bDate = new Date(bVal);
            return sortDirection === 'asc' ? aDate - bDate : bDate - aDate;
        }

        // For booleans (columns 5, 6: published, locked)
        if (columnIndex === 5 || columnIndex === 6) {
            const aBool = aVal === true || aVal === 'true' || aVal === 'True' || aVal === '1';
            const bBool = bVal === true || bVal === 'true' || bVal === 'True' || bVal === '1';
            const aNum = aBool ? 1 : 0;
            const bNum = bBool ? 1 : 0;
            return sortDirection === 'asc' ? aNum - bNum : bNum - aNum;
        }

        // For strings (title, path, type)
        const aStr = String(aVal).toLowerCase();
        const bStr = String(bVal).toLowerCase();
        return sortDirection === 'asc' ? aStr.localeCompare(bStr) : bStr.localeCompare(aStr);
    }, [sortDirection]);

    // Sort data
    const sortedData = useMemo(() => {
        if (!data || !Array.isArray(data.data) || sortColumn === null) {
            return data?.data || [];
        }

        const sorted = [...data.data].sort((a, b) => compareValues(a[sortColumn], b[sortColumn], sortColumn));

        return sorted;
    }, [data, sortColumn, compareValues]);

    // Paginate data (currentPage is 1-based, so subtract 1 for array index)
    const paginatedData = useMemo(() => {
        if (!sortedData || !Array.isArray(sortedData)) {
            return [];
        }

        const startIndex = (currentPage - 1) * rowsPerPage;
        return sortedData.slice(startIndex, startIndex + rowsPerPage);
    }, [sortedData, currentPage, rowsPerPage]);

    if (!data || !Array.isArray(data.data) || data.data.length === 0) {
        console.log('ReportResultsTable - No data to display');
        return (
            <div className={styles.noResults}>
                <Typography variant="body" weight="bold">No results found</Typography>
            </div>
        );
    }

    console.log('ReportResultsTable - Rendering table with', data.data.length, 'rows');

    const exportToCSV = () => exportDataToCSV(data.data, effectiveColumns, useCustomColumns);
    const exportToJSON = () => exportDataToJSON(data);

    return (
        <div className={styles.tableContainer}>
            <div className={styles.resultsHeader}>
                <div className={styles.resultsInfo}>
                    <Typography variant="body" weight="bold">
                        {data.recordsTotal || 0} results found
                    </Typography>
                </div>
                <div className={styles.exportButtons}>
                    <Button
                        size="default"
                        variant="outlined"
                        label="Export CSV"
                        icon={<Download/>}
                        onClick={exportToCSV}
                    />
                    <Button
                        size="default"
                        variant="outlined"
                        label="Export JSON"
                        icon={<Download/>}
                        onClick={exportToJSON}
                    />
                </div>
            </div>
            <Table className={styles.table}>
                <TableHead>
                    <TableRow>
                        <TableHeaders
                            useCustomColumns={useCustomColumns}
                            columns={effectiveColumns}
                            sortColumn={sortColumn}
                            sortDirection={sortDirection}
                            handleSort={handleSort}
                            t={t}
                            siteLanguages={siteLanguages}
                        />
                    </TableRow>
                </TableHead>
                <TableBody>
                    {paginatedData.map((row, rowIndex) => {
                        const rowKey = `row-${rowIndex}-${row[0] || rowIndex}`;
                        return (
                            <TableRow
                                key={rowKey}
                                onMouseEnter={event => {
                                    if (reportId === '28' && isImageMimeType(row[0]) && row[2]) {
                                        setImagePreview({
                                            rowKey,
                                            src: buildAssetPreviewUrl(row[2], row[5]),
                                            x: event.clientX + 16,
                                            y: event.clientY + 16
                                        });
                                    } else {
                                        setImagePreview(null);
                                    }
                                }}
                                onMouseMove={event => {
                                    if (imagePreview && imagePreview.rowKey === rowKey) {
                                        setImagePreview(prev => prev ? ({
                                            ...prev,
                                            x: event.clientX + 16,
                                            y: event.clientY + 16
                                        }) : prev);
                                    }
                                }}
                                onMouseLeave={() => setImagePreview(null)}
                            >
                                {useCustomColumns ? (
                                    // Render custom columns based on report configuration
                                    effectiveColumns.map((column, colIndex) => {
                                        const cellValue = row[colIndex];
                                        const isPathColumn = column.key && column.key.toLowerCase().includes('path');
                                        const cellTitle = isPathColumn && typeof cellValue === 'string' && cellValue ? cellValue : undefined;
                                        const cellClassName = column.type === 'date' ? styles.dateCell :
                                            column.type === 'boolean' ? styles.booleanCell :
                                            column.type === 'icon' ? styles.mimeIconCell :
                                            (column.isLanguage || isPathColumn) ? styles.pathCell :
                                            (column.type === 'html' && column.noWrap === false) ? styles.htmlCell :
                                            undefined;
                                        return (
                                            <TableBodyCell
                                                key={`${rowKey}-${column.key}`}
                                                className={cellClassName}
                                                title={cellTitle}
                                            >
                                                {renderCellValue(cellValue, column.type, row, column.key)}
                                            </TableBodyCell>
                                        );
                                    })
                                ) : (
                                    // Default columns for backward compatibility
                                    <>
                                        <TableBodyCell className={styles.titleCell}>{row[0] || '-'}</TableBodyCell>
                                        <TableBodyCell className={styles.pathCell} title={row[1] || undefined}>
                                            {renderCellValue(row[1], 'link', row, 'path')}
                                        </TableBodyCell>
                                        <TableBodyCell>{row[2] || '-'}</TableBodyCell>
                                        <TableBodyCell className={styles.dateCell}>{formatDate(row[3])}</TableBodyCell>
                                        <TableBodyCell className={styles.dateCell}>{formatDate(row[4])}</TableBodyCell>
                                        <TableBodyCell className={styles.booleanCell}>{renderBooleanValue(row[5])}</TableBodyCell>
                                        <TableBodyCell className={styles.booleanCell}>{renderBooleanValue(row[6])}</TableBodyCell>
                                    </>
                                )}
                            </TableRow>
                        );
                    })}
                </TableBody>
            </Table>
            <TablePagination
                currentPage={currentPage}
                totalNumberOfRows={sortedData.length}
                rowsPerPage={rowsPerPage}
                rowsPerPageOptions={[10, 25, 50, 100]}
                label={{
                    rowsPerPage: 'Rows per page:',
                    of: 'of'
                }}
                onPageChange={setCurrentPage}
                onRowsPerPageChange={setRowsPerPage}
            />
            {imagePreview && imagePreview.src && imagePreview.x !== null && imagePreview.y !== null && (
                <div className={styles.rowPreview} style={{top: imagePreview.y, left: imagePreview.x}}>
                    <img src={imagePreview.src} alt="" className={styles.rowPreviewImage}/>
                </div>
            )}
        </div>
    );
};

ReportResultsTable.propTypes = {
    data: PropTypes.shape({
        recordsTotal: PropTypes.number,
        recordsFiltered: PropTypes.number,
        data: PropTypes.arrayOf(PropTypes.array),
        siteLanguages: PropTypes.arrayOf(PropTypes.string)
    }),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string,
    columns: PropTypes.arrayOf(PropTypes.shape({
        key: PropTypes.string,
        labelKey: PropTypes.string,
        sortable: PropTypes.bool,
        type: PropTypes.string
    })),
    reportId: PropTypes.string,
    reportType: PropTypes.string
};

export default ReportResultsTable;
