$(document).ready(function () {
    var feedbackTable = $('#feedbackTable').DataTable({
        "paging": false,
        "ordering": true,
        "info": true,
        "searching": true,
        "order": [[2, 'asc']],
        "columnDefs": [
            {
                searchable: false,
                orderable: false,
                targets: 0
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
                    exportOptions: {columns: ':not(:first-child)'},
                    title: 'Feedback'
                },
                {
                    extend: 'csvHtml5',
                    exportOptions: {columns: ':not(:first-child)'},
                    text: 'TSV',
                    fieldSeparator: '\t',
                    extension: '.tsv',
                    title: 'Feedback'
                },
                {
                    extend: 'copyHtml5',
                    exportOptions: {columns: ':not(:first-child)'},
                    messageTop: null,
                    title: null,
                },
            ]
        },
        "language": {
            "emptyTable": "No data available in table",
            "infoFiltered": "(from _MAX_ total feedbacks)",
            "info": "_TOTAL_ feedback(s) found",
            "infoEmpty": "No feedback found",
            "thousands": "."
        },
        dom: '<"row justify-content align-items-end"<"col"B><"col text-right"iPf>>t'
    });

    feedbackTable.on('order.dt search.dt', function () {
        feedbackTable.column(0, {search: 'applied', order: 'applied'}).nodes().each(function (cell, i) {
            cell.innerHTML = i + 1;
        });
    }).draw();
});
