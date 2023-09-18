document.addEventListener("DOMContentLoaded", function(event) {
    convertTable('#active-requests', '/admin/requested-values/get-active-requests', activeColumns);
    convertTable('#finished-requests', '/admin/requested-values/get-finished-requests', finishedColumns);
});

let activeColumns = [
    { data: "fieldName" },
    { data: "requestedValue" },
    { data: "requester" },
    {
        data: "originSubmission",
        render: function (data) {
            return `<a href="/metadata-validator/submission/simple/admin?identifier=${data}" target="_blank">${data}</a>`;
        }
    },
    { data: "usedSubmissions" },
    { data: "formattedDateCreated" },
    {
        data: "id",
        render: function (data) {
            return `<i class="far fa-edit cursor-pointer requestedValuesModal" 
                       data-toggle="modal" 
                       data-target="#requestedValuesModal" 
                       onclick="document.getElementById('requestedValueId').value = ${data}"></i>`;
        }
    },
];

let finishedColumns = [
    { data: "fieldName" },
    { data: "requestedValue" },
    { data: "requester" },
    {
        data: "originSubmission",
        render: function (data) {
            return `<a href="/metadata-validator/submission/simple/admin?identifier=${data}" target="_blank">${data}</a>`;
        }
    },
    { data: "usedSubmissions" },
    { data: "formattedDateCreated" },
    {
        data: "createdValue",
        render: function (data) {
            if (data === "REJECTED") return '<i class="fas fa-times text-danger" title="Value was rejected"></i>';
            return data;
        }
    },
    {
        data: { id: "id", createdValue: "createdValue" },
        render: function (data) {
            if (data.createdValue === "REJECTED") return "";
            return `<i class="far fa-edit cursor-pointer requestedValuesModal" 
                       data-toggle="modal" 
                       data-target="#requestedValuesModal" 
                       onclick="document.getElementById('requestedValueId').value = ${data.id}; 
                                document.getElementById('createdValueAs').value = '${data.createdValue}';"></i>`;
        }
    },
];

function convertTable(table, url, columnArray) {
    $(table).DataTable({
        "dom": '<"row justify-content align-items-end"<"col text-right"iPf>>t',
        "paging": false,
        "ordering": true,
        "info": true,
        "searching": true,
        "ajax": url,
        "order": [[3, 'asc'], [5, 'asc']],
        "columns": columnArray,
        "columnDefs": [
            {
                searchable: false,
                orderable: false,
                targets: 'not-sortable'
            }
        ],
        "language": {
            "emptyTable": "No data available in table",
            "infoFiltered": "(from _MAX_ total requested values)",
            "info": "_TOTAL_ requested value(s) found",
            "infoEmpty": "No requested values found",
            "thousands": "."
        },
    });
}
