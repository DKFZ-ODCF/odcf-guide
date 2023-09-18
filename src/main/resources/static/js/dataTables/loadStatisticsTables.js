$(document).ready(function() {
    let tables = document.getElementsByTagName('table');
    for (let i = 0; i < tables.length; i++) {
        let table = tables[i];
        executeGenerateTableAjax(table.getAttribute('id'), "/admin/statistics/get-");
    }
});

function generateStatisticsTable(tableName, data) {
    let table = document.getElementById(tableName);
    let body = table.getElementsByTagName('tbody')[0];
    let spinner = body.getElementsByClassName('spinner-border')[0];

    if (data.length === 0) {
        let tr = document.createElement('tr');
        tr.classList.add("active-row");
        tr.innerHTML = '<td>There are no submissions available</td>';
        body.appendChild(tr);
    }

    data.forEach(function(item) {
        let tr = document.createElement('tr');
        tr.classList.add("active-row");
        tr.innerHTML = body.getElementsByClassName('blank_row')[0].innerHTML
            .replaceAll("SUBMISSIONS", item.total_submissions)
            .replaceAll("SUBMISSION_WAC", item.submissions_without_auto_closed)
            .replaceAll("STATE", item.state)
            .replaceAll("SAMPLES", item.total_samples)
            .replaceAll("SAMPLE_WAC", item.samples_without_auto_closed)
            .replaceAll("MONTH", item.month);
        body.appendChild(tr);
    });
    spinner.remove();
    convertTable(table);
}
function convertTable(table) {
    let dataTable = $(table).DataTable({
        "dom": 'Blrtip',
        "paging": false,
        "ordering": false,
        "info": false,
        "searching": false,
        buttons: [
            {
                extend: 'csvHtml5',
                exportOptions: {rows: ':visible'},
            },
            {
                extend: 'csvHtml5',
                exportOptions: {rows: ':visible'},
                text: 'TSV',
                fieldSeparator: '\t',
                extension: '.tsv',
            },
            {
                extend: 'copyHtml5',
                exportOptions: {rows: ':visible'},
                messageTop: null,
            },
        ],
        "footerCallback": function ( row, data, start, end, display ) {
            var api = this.api();
            let intVal = function (i) { return parseInt(i) || 0;};
            let cells = row.getElementsByClassName('sum');
            for (let cell of cells) {
                let index = cell.cellIndex;
                let total = api.column(index).data().reduce(function (a, b) {
                    return intVal(a) + intVal(b);
                }, 0);
                $(api.column(index).footer()).html(total);
            }
        }
    });
    dataTable.buttons().nodes().each(function () {
        $(this).removeClass('btn-secondary').addClass('btn-outline-secondary');
    });
}
