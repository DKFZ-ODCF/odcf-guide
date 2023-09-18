$(document).ready(function() {
    var parserTable = $('#parserOverviewTable').DataTable( {
        "paging": false,
        "ordering": true,
        "info": true,
        "searching": true,
        "order": [[ 1, 'asc' ]],
        "columnDefs": [
            {
                searchable: false,
                orderable: false,
                targets: [0, -1]
            }
        ],
        buttons: {
            dom: {
                button: {
                    className: 'btn btn-outline-secondary' //Primary class for all buttons
                }
            },
            buttons: [
                {
                    extend: 'csvHtml5',
                    exportOptions: { columns: ':not(:first-child)' },
                    title: 'Parser'
                },
                {
                    extend: 'csvHtml5',
                    exportOptions: { columns: ':not(:first-child)' },
                    text: 'TSV',
                    fieldSeparator: '\t',
                    extension: '.tsv',
                    title: 'Parser'
                },
                {
                    extend: 'copyHtml5',
                    exportOptions: { columns: ':not(:first-child)' },
                    messageTop: null,
                    title: null,
                },]
        },
        "language": {
            "emptyTable": "No data available in table",
            "infoFiltered": "(from _MAX_ total parsers)",
            "info": "_TOTAL_ parser(s) found",
            "infoEmpty": "No parser found",
            "thousands": "."
        },
        dom: '<"row justify-content align-items-end"<"col"B><"col text-right"iPf>>t'
    });

    parserTable.on( 'order.dt search.dt', function () {
        parserTable.column(0, {search:'applied', order:'applied'}).nodes().each( function (cell, i) {
            cell.innerHTML = i+1;
        } );
    } ).draw();
});
