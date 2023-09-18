function changeSeqTypeData(seqTypeId, seqType, basicSeqType, isSingleCell, needAntibodyTarget, needLibPrepKit, tagmentation, isDisplayedForUser, importAlias) {
    $('#seqTypeName').val(seqType);
    $('#seqTypeId').val(seqTypeId);
    $('#basicSeqType').val(basicSeqType);
    $('#singleCell').attr('checked', (isSingleCell === "true"));
    $('#needAntibodyTarget').attr('checked', (needAntibodyTarget === "true"));
    $('#needLibPrepKit').attr('checked', (needLibPrepKit === "true"));
    $('#tagmentation').attr('checked', (tagmentation === "true"));
    $('#isDisplayedForUser').attr('checked', (isDisplayedForUser === "true"));

    if (importAlias !== '[]') {
        var importAliases = importAlias.replace(/[\[\]]+/g,'').split(", ");
        for (let elem in importAliases) {
            $('.ilseNames').eq( elem ).val(importAliases[elem]);
            addImportAliases($('.btn-add'));
        }
    } else {
        $('#ilseNames').val('');
    }

    $('#submitSeqType').text("Save");
    document.getElementById('deleteSeqType').href = `/metadata-input/delete-seq-type?id=${seqTypeId}`;
    document.getElementById('deleteSeqType').setAttribute("onclick", `return confirm_seq_type_deletion('${seqType}');`);

    $("#seqTypeData").get(0).scrollIntoView();
}

function confirm_seq_type_deletion(name) {
    return confirm("DELETE seq type '"+name+"'?");
}

document.addEventListener("DOMContentLoaded", function(event) {
    $('#requested-seq-types').DataTable({
        "paging": false,
        "ordering": true,
        "info": false,
        "searching": false,
        "ajax": '/admin/requested-values/get-requested-seq-types',
        "order": [[0, 'asc'], [6, 'asc']],
        "columns": [
            { data: "seqTypeName" },
            { data: "basicSeqType" },
            {
                data: "singleCell",
                render: function (data) {
                    if (data) return `<i class="fas fa-check text-success"></i>`;
                    return `<i class="fas fa-times text-danger"></i>`;
                }
            },
            {
                data: "needAntibodyTarget",
                render: function (data) {
                    if (data) return `<i class="fas fa-check text-success"></i>`;
                    return `<i class="fas fa-times text-danger"></i>`;
                }
            },
            {
                data: "needLibPrepKit",
                render: function (data) {
                    if (data) return `<i class="fas fa-check text-success"></i>`;
                    return `<i class="fas fa-times text-danger"></i>`;
                }
            },
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
                data: "id" ,
                render: function (data) {
                    return `<i class="far fa-edit cursor-pointer requestedValuesModal" 
                       data-toggle="modal" 
                       data-target="#requestedValuesModal" 
                       onclick="document.getElementById('requestedValueId').value = ${data}"></i>`;
                }
            },
        ],
        "columnDefs": [
            {
                searchable: false,
                orderable: false,
                targets: 'not-sortable'
            }
        ],
    });
});
