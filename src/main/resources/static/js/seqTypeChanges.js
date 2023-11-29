document.addEventListener("DOMContentLoaded", function() {
    initializeSeqTypeChangesDropdown();
    convertAllSeqTypesTable();
    convertRequestedSeqTypesTable();
});

function changeSeqTypeData(seqTypeId) {
    $.getJSON("/get-seq-type-data", {
        seqTypeId : seqTypeId,
        ajax : 'true'
    }, function(seqType) {
        if (seqType != null) {
            document.getElementById("seqTypeData").reset();
            $('#seqTypeName').val(seqType.name);
            $('#seqTypeId').val(seqType.id);
            $('#basicSeqType').val(seqType.basicSeqType);

            let ilseNamesSelectize = document.querySelector('select#ilseNames.selectized').selectize;
            ilseNamesSelectize.clear(true);
            seqType.importAliases.forEach(elem => ilseNamesSelectize.createItem(elem, false));

            let seqTypeOptionsSelectize = document.querySelector('select#seqTypeOptions.selectized').selectize;
            seqTypeOptionsSelectize.clear(true);
            let seqTypeOptions = new Map(Object.entries(JSON.parse(seqType.json)));
            seqTypeOptions.forEach((value, key) => {
                if (value) seqTypeOptionsSelectize.addItem(key, true);
            });
        }
        document.getElementById('deleteSeqType').setAttribute("onclick", `return confirm_seq_type_deletion('${seqType.name}');`);
    });

    document.getElementById('deleteSeqType').href = `/metadata-input/delete-seq-type?id=${seqTypeId}`;
    $("#seqTypeData").get(0).scrollIntoView();
}

function initializeSeqTypeChangesDropdown() {
    let ilseNamesDropdown = document.querySelector("select.selectize-ilseNames");
    if (ilseNamesDropdown.selectize === undefined) {
        $(ilseNamesDropdown).selectize({
            plugins: ["remove_button"],
            delimiter: ",",
            create: true,
            persist: false,
            openOnFocus: false,
            closeAfterSelect: true,
        });
    }

    let seqTypeOptionsDropdown = document.querySelector("select.selectize-seqTypeOptions");
    if (seqTypeOptionsDropdown.selectize === undefined) {
        $(seqTypeOptionsDropdown).selectize({
            plugins: ["remove_button"],
            delimiter: ",",
            create: false,
            persist: false,
            closeAfterSelect: false,
            onInitialize: function () {
                this.clear(true);
                let items = this.$input.data("items")?.split(',');
                if (items !== undefined) {
                    if (items[0] !== "" || items[0] !== "empty") {
                        for (let i = 0; i < items.length; i++) {
                            this.addItem(items[i], true);
                        }
                    }
                }
            },
        });
    }
}

function convertAllSeqTypesTable() {
    $("#allSeqTypes-table").DataTable({
        "dom": '<"row justify-content align-items-end"<"col text-right"iPf>>t',
        "paging": false,
        "ordering": true,
        "order": [0, 'asc'],
        "info": true,
        "searching": true,
        "ajax": "/admin/sequencing-type/get-seq-types",
        "columns": [
            { data: "name" },
            {
                data: "importAliases",
                render: function (data) {
                    let render = "";
                    data.forEach( elem => render += `<span class="badge badge-dark mr-1 big-badge">${elem}</span>` );
                    return render;
                }
            },
            { data: "basicSeqType" },
            {
                data: "seqTypeOptions",
                render: function (data) {
                    let render = "";
                    const seqTypeOptions = new Map(Object.entries(data));
                    seqTypeOptions.forEach((value, key) => {
                        key = key.replace("!", '<i class="fas fa-times"></i> ');
                        if (value) render += `<span class="badge badge-info mr-1 big-badge">${key}</span>`;
                    });
                    return render;
                }
            },
            {
                data: "id",
                render: function (data) {
                    return `<a class="far fa-edit cursor-pointer" onclick="changeSeqTypeData(${data})"></a>`;
                }
            },
        ],
        "columnDefs": [
            {
                searchable: false,
                orderable: false,
                targets: 'not-sortable'
            },
            {
                className: "text-center",
                targets: [2, 4]
            }
        ],
        "language": {
            "emptyTable": "No data available in table",
            "infoFiltered": "(from _MAX_ total seqTypes)",
            "info": "_TOTAL_ seqType(s) found",
            "infoEmpty": "No seqTypes found",
            "thousands": "."
        },
    });
}

function confirm_seq_type_deletion(name) {
    return confirm("DELETE seq type '"+name+"'?");
}

function convertRequestedSeqTypesTable() {
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
                data: "seqTypeOptions",
                render: function (data) {
                    let render = "";
                    const seqTypeOptions = new Map(Object.entries(data));
                    seqTypeOptions.forEach((value, key) => {
                        if (value) render += `<span class="badge badge-info mr-1 big-badge">${key}</span>`;
                    });
                    return render;
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
}
