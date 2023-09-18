function initSelectizeWithRequestOption() {
    sessionStorage.removeItem("requestedValues");
    let sessionStorageMap = new Map();

    let dropdowns = document.querySelectorAll('select.selectize-request');
    for (let dropdown of dropdowns) {
        if (dropdown.selectize === undefined && !dropdown.closest('table').hidden) {
            $(dropdown).selectize({
                create: function (newValue) {
                    let newValueTemp = newValue + "(ReqVal)";
                    let fieldName = this.$input[0].dataset.fieldname;

                    processNewValueSimilarity(newValueTemp, fieldName, this);
                    return { value: newValueTemp, text: newValueTemp };
                },
                plugins: ["restore_on_backspace"],
                persist: true,
                closeAfterSelect: true,
                dropdownParent: 'body',
                openOnFocus: false,
                createOnBlur: true,
                onInitialize: function () {
                    this.clear(true);
                    let selectedValue = this.$input.data("selected-value");
                    if (selectedValue === undefined || selectedValue === "" || selectedValue === "empty") {
                        return;
                    }
                    let foundOption = Object.entries(this.options).find(option => option[1].text === selectedValue + '(ReqVal)' || option[1].text === selectedValue);
                    if (foundOption !== undefined) {
                        this.addItem(foundOption[0], true);
                    } else {
                        let fieldName = this.$input[0].dataset.fieldname;
                        let fieldValues = new Set(sessionStorageMap.get(fieldName)?.split("; "));
                        if (fieldValues === undefined) { fieldValues = new Set(); }
                        fieldValues.add(selectedValue);
                        sessionStorageMap.set(fieldName, [...fieldValues].join("; "));

                        /*let value = selectedValue + "(ReqVal)";
                        this.addOption({ value: value , text: value });
                        this.addItem(value, true);
                        processNewValueRequest(value, 'select.selectize-request.selectized[data-fieldName = ' + this.$input[0].dataset.fieldname + ']');*/
                    }
                },
                render: {
                    item: function (item, escape) {
                        return "<div>" +
                            (item.text ? '<span class="name">' + escape(item.text.replace('(ReqVal)', '')) + "</span>" : "") +
                            (item.value.endsWith('(ReqVal)') ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option: function (item, escape) {
                        return "<div class='option option-wrap'>" +
                            (item.text ? '<span class="name">' + escape(item.text.replace('(ReqVal)', '')) + "</span>" : "") +
                            (item.value.endsWith('(ReqVal)') ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option_create: function(data, escape) {
                        return '<div class="create">Request: <strong>' + escape(data.input) + '</strong></div>';
                    }
                },
                onChange: function(value) {
                    if (this.$input[0].dataset.fieldname === "center") {
                        processCenterForIlseNumber(this.$input, value);
                    }
                    if (this.$input[0].dataset.fieldname === "antibodyTarget") {
                        updateSampleTypeSuffix(this.$input[0]);
                    }
                }
            });
        }
    }
    sessionStorage.setItem("requestedValues", JSON.stringify(Array.from(sessionStorageMap.entries())));
}

/**
 * Checks whether a similar value already exists and displays it to the user.
 * Run {@link processNewValueRequest} if the requested species is new.
 * @param {string} newValue New value being requested
 * @param {string} fieldName Field name of the new value (i.e. center, instrumentModel)
 * @param {Object} selectizeObject selectize object where the new value has to be removed in the dropdown if the process of requesting a value is aborted
 */
function processNewValueSimilarity(newValue, fieldName, selectizeObject) {
    let selector = 'select.selectize-request.selectized[data-fieldName = ' + fieldName + ']';

    $.getJSON("/new-value-similarity-check", {
        newValue: newValue,
        fieldName: fieldName,
        ajax: 'true'
    }, function (resultJson) {
        postProcessNewValueSimilarity(resultJson, newValue, selector, selectizeObject);
    });
}

function postProcessNewValueSimilarity(resultJson, newValue, selector, selectizeObject) {
    let status = resultJson.status;
    if (status === "2" || status === "1") {
        $('#similarValuesText').html(resultJson.response);
        let button = $('#similarValuesConfirmButton');
        button.one('click', function () {
            if (selector.includes('seqType')) {
                dealWithSeqType(newValue);
            } else {
                processNewValueRequest(newValue, selector);
            }
        });

        button.toggle(status === "2");
        let similarityModal = $('#similarElementAlreadyExistsDiv');
        similarityModal.modal('show');
        removeEntryOnClose(similarityModal, selectizeObject, newValue);
        return;
    }
    if (selector.includes('seqType')) {
        dealWithSeqType(newValue);
    } else {
        processNewValueRequest(newValue, selector);
    }
}

async function processNewValueRequest(newValue, selector) {
    $('#similarElementAlreadyExistsDiv').modal('hide');

    document.querySelectorAll(selector).forEach(dropdown => {
        let newValueTemp = newValue;
        if (!newValue.endsWith("(ReqVal)")) {
            newValueTemp = newValue + "(ReqVal)";
        }
        let selectize = dropdown.selectize;
        selectize.addOption({ value: newValueTemp, text: newValue });
    });
}

function removeEntryOnClose(modal, selectizeObject, newValue) {
    modal.one('hide.bs.modal', function () {
        if (!$(document.activeElement).hasClass("success-request")) {
            selectizeObject.removeOption(newValue);
        }
    });
}

function dealWithSeqType(newValue) {
    document.getElementById('seqTypeNameForRequest').value = newValue;
    $('#similarElementAlreadyExistsDiv').modal('hide');
    $('#requestNewSeqTypeModal').modal('show');
}
