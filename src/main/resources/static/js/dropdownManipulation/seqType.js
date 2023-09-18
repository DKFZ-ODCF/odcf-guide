function initSelectizeSeqType() {
    let dropdowns = document.querySelectorAll('select.selectize-seqType');
    for (let dropdown of dropdowns) {
        if (dropdown.selectize === undefined && !dropdown.closest('table').hidden) {
            $(dropdown).selectize({
                create: function (newSeqType) {
                    processNewSeqTypeSimilarity(newSeqType, this);
                    return {
                        value: -1,
                        text: newSeqType,
                        optgroup: null,
                        data: {
                            singleCell: null,
                            needAntibodyTarget: null,
                            needLibPrepKit: null,
                            tagmentation: null,
                            isRequested: true,
                        },
                    };
                },
                plugins: ["restore_on_backspace"],
                persist: false,
                dropdownParent: 'body',
                openOnFocus: false,
                createOnBlur: true,
                render: {
                    item: function (item, escape) {
                        return "<div data-single-cell='" + escape(item.data.singleCell) +
                            "' data-need-antibody-target='" + escape(item.data.needAntibodyTarget) +
                            "' data-need-lib-prep-kit='" + escape(item.data.needLibPrepKit) +
                            "' data-need-sample-type-category='" + escape(item.data.needSampleTypeCategory) +
                            "' data-tagmentation='" + escape(item.data.tagmentation) +
                            "' data-low-coverage-requestable='" + escape(item.data.lowCoverageRequestable) +
                            "'>" +
                            (item.text ? '<span class="name">' + escape(item.text) + "</span>" : "") +
                            (item.optgroup ? '&nbsp;<small class="text-muted align-text-top">[' + escape(item.optgroup) + "]</small>" : "") +
                            (item.data.isRequested ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option: function (item, escape) {
                        return "<div class='option' data-single-cell='" + escape(item.data.singleCell) +
                            "' data-need-antibody-target='" + escape(item.data.needAntibodyTarget) +
                            "' data-need-lib-prep-kit='" + escape(item.data.needLibPrepKit) +
                            "' data-need-sample-type-category='" + escape(item.data.needSampleTypeCategory) +
                            "' data-tagmentation='" + escape(item.data.tagmentation) + "'>" +
                            (item.text ? '<span class="name">' + escape(item.text) + "</span>" : "") +
                            (item.data.isRequested ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option_create: function(data, escape) {
                        return '<div class="create">Request: <strong>' + escape(data.input) + '</strong></div>';
                    }
                },
                onItemAdd: function(value, item) {
                    if (item[0].closest('tr').id !== 'bulkEditRow') {
                        seqTypesChanger(item, true);
                    }
                },
                onInitialize: function () {
                    if (this.items.length > 0) {
                        seqTypesChanger(this.getItem(this.items[0]), false);
                    }
                }
            });
        }
    }
}

function seqTypesChanger(item, runDecideToShow) {
    let elem = item[0];
    processAntibodyTargetDropdownStatus(elem, elem.dataset.needAntibodyTarget === 'true', runDecideToShow);
    processTagmentationLibraryRequired(elem, elem.dataset.tagmentation === 'true', runDecideToShow);
    processLowCoverageRequested(elem, elem.dataset.lowCoverageRequestable === 'true', runDecideToShow);
    processLibPrepKitRequired(elem, elem.dataset.needLibPrepKit === 'true');
    processSampleTypeCategory(elem, elem.dataset.needSampleTypeCategory === 'true', runDecideToShow);
    performSingleCellWellLabelUpdate(elem, elem.dataset.singleCell === 'true', elem.getElementsByClassName('name')[0].innerText, runDecideToShow);
}

/**
 * Checks whether a similar seq type already exists and displays it to the user.
 * @param {string} newSeqType
 * @param {Object} selectizeObject
 */
function processNewSeqTypeSimilarity(newSeqType, selectizeObject) {
    let selector = 'select.selectize-seqType.selectized';

    $.getJSON("/new-seq-type-similarity-check", {
        newSeqType : newSeqType,
        ajax : 'true'
    }, function(resultJson) {
        postProcessNewValueSimilarity(resultJson, newSeqType, selector, selectizeObject);
    });
}

function processSeqTypeRequest() {
    let newSeqType = document.getElementById("seqTypeNameForRequest").value;
    let basicSeqType = document.getElementById("basicSeqTypeNameForRequest").value;
    let singleCell = document.getElementById("singleCellForRequest").checked;
    let needAntibodyTarget = document.getElementById("needAntibodyTargetForRequest").checked;

    if (basicSeqType === "-- select --") {
        $('#basicSeqTypeNameForRequest').addClass("is-invalid");
        return false;
    }
    $('#basicSeqTypeNameForRequest').removeClass("is-invalid");

    $.ajax({
        type: "POST",
        url: "/admin/requested-values/request-seq-type",
        data: {
            name: newSeqType,
            basicSeqType: basicSeqType,
            singleCell: singleCell,
            needAntibodyTarget: needAntibodyTarget,
        },
        dataType: 'text',
        timeout: 60000,
        success: function (savedSeqTypeId) {
            $('#requestNewSeqTypeModal').modal('hide');
            let optgroup = basicSeqType;
            if (singleCell) { optgroup += " single cell"; }
            let bulkedit = false;

            document.querySelectorAll('select.selectize-seqType.selectized').forEach(dropdown => {
                let selectize = dropdown.selectize;
                selectize.addOption({
                    value: savedSeqTypeId,
                    text: newSeqType,
                    optgroup: optgroup,
                    data: {
                        singleCell: singleCell,
                        needAntibodyTarget: needAntibodyTarget,
                        needLibPrepKit: false,
                        tagmentation: false,
                        isRequested: true,
                    },
                });

                if (dropdown.classList.contains('seqType-bulk-edit') && selectize.$input[0].value === "-1") {
                    selectize.removeItem("-1", true);
                    bulkedit = true;
                }

                if (!dropdown.classList.contains('seqType-bulk-edit')) {
                    if (selectize.$input[0].value === "-1" || bulkedit) {
                        selectize.removeItem("-1", true);
                        selectize.addItem(savedSeqTypeId, false);
                    }
                }
            });
            document.getElementById("requestSeqTypeForm").reset();
        },
        error: function (e) {
            console.log("SEQ TYPE REQUEST ERROR :", e);
        }
    });
}
