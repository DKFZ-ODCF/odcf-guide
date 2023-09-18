/**
 * Exports the table data.
 * @param table_id The id of the table to export.
 * @param separator The separator to use for the export file.
 * @param type The type to use for the export file.
 */
function export_table_as_csv(table_id, separator = ',', type = 'csv') {
    let rows = document.querySelectorAll('table#' + table_id + ' tr.sample-data-row:not(.text-blue), tr.sample-header-row');
    let csv = [];
    for (let i = 0; i < rows.length; i++) {
        let row = [], cols = rows[i].querySelectorAll('td:not(.ignore-export), th:not(.ignore-export)');
        for (let j = 0; j < cols.length; j++) {
            let data = cols[j].innerText.replace(/(\r\n|\n|\r)/gm, '')
                .replace(/(\s\s)/gm, ' ')
                .replace(/"/g, '""');
            row.push('"' + data + '"');
        }
        csv.push(row.join(separator));
    }
    let csv_string = csv.join('\n');
    let filename = 'export_' + document.getElementById('identifier').innerText + '.' + type;
    let link = document.createElement('a');
    link.style.display = 'none';
    link.setAttribute('target', '_blank');
    link.setAttribute('href', 'data:text/' + type + ';charset=utf-8,' + encodeURIComponent(csv_string));
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
