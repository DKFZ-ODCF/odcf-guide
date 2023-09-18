document.addEventListener("DOMContentLoaded", function() {
    $("#allValidation-table").DataTable({
        "dom": '<"row justify-content align-items-end"<"col text-right"iPf>>t',
        "paging": false,
        "ordering": true,
        "order": [0, 'asc'],
        "info": true,
        "searching": true,
        "ajax": "/admin/validation/get-all",
        "columns": [
            { data: "validationName" },
            { data: "regex" },
            {
                data: "required",
                render: function (data) {
                    let required = ((data) ? "fa-check text-success" : "fa-times text-danger");
                    return `<i class="fas ${required}"></i>`;
                }
            },
            { data: "description" },
            {
                data: "id",
                render: function (data) {
                    return `<a class="far fa-edit cursor-pointer" onclick="modifyValidationData(${data})"></a>`;
                }
            },
        ],
        "columnDefs": [
            {
                orderable: false,
                targets: "_all"
            }
        ],
        "language": {
            "emptyTable": "No data available in table",
            "infoFiltered": "(from _MAX_ total validation fields)",
            "info": "_TOTAL_ validation fields(s) found",
            "infoEmpty": "No validation fields found",
            "thousands": "."
        },
    });
});


function modifyValidationData(validationId) {
    $.getJSON("/admin/validation/get-data", {
        validationId : validationId,
        ajax : 'true'
    }, function(validation) {
        if (validation != null) {
            document.getElementById("validationData").reset();
            $('#validationId').val(validation.id);
            $('#validationName').val(validation.field);
            $('#validationRegex').val(validation.regex);
            $('#validationDescription').val(validation.description.replaceAll("<br>", '\n'));
            $('#validationRequired').attr('checked', validation.required);
        }
    });

    $('#modifyValidationModal').modal('show');
}
