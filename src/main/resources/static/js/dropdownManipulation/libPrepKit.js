function initSelectizeLibPrepKit() {
    let dropdowns = document.querySelectorAll('select.selectize-libPrepKit');
    for (let dropdown of dropdowns) {
        if (dropdown.selectize === undefined && !dropdown.closest('table').hidden) {
            $(dropdown).selectize({
                create: function (libPrepKit) {
                    let newValueTemp = libPrepKit + "(ReqVal)";
                    processNewValueSimilarity(newValueTemp, "libraryPreparationKit", this);
                    return {
                        value: newValueTemp,
                        text: newValueTemp,
                        adapterSequence: "empty",
                    };
                },
                plugins: ["restore_on_backspace"],
                persist: false,
                dropdownParent: 'body',
                openOnFocus: false,
                createOnBlur: true,
                respect_word_boundaries: false,
                render: {
                    item: function (item, escape) {
                        let adapterSequence = item.adapterSequence;
                        return `<div data-adapter-sequence='${escape(adapterSequence)}'>` +
                            (item.text ? `<span class="name">${escape(item.text.replace('(ReqVal)', ''))}</span>` : "") +
                            showAdapterSequence(adapterSequence) +
                            (item.value.endsWith('(ReqVal)') ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option: function (item, escape) {
                        let adapterSequence = item.adapterSequence;
                        return `<div class='option option-wrap' data-adapter-sequence='${escape(adapterSequence)}'>` +
                            (item.text ? `<span class="name">${escape(item.text.replace('(ReqVal)', ''))}</span>` : "") +
                            showAdapterSequence(adapterSequence) +
                            (item.value.endsWith('(ReqVal)') ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option_create: function(data, escape) {
                        return '<div class="create">Request: <strong>' + escape(data.input) + '</strong></div>';
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

function showAdapterSequence(adapterSequence) {
    if (adapterSequence !== undefined && adapterSequence !== "") {
        let regex = /^.*\n([ATGCN]+)[\s\S]*$/;
        let sequenceSlice = adapterSequence.replace(regex, (_, secondLine) => {
            return secondLine.slice(0, 4) + '...';
        });

        return `&nbsp;<small class="text-muted align-text-top" title="${adapterSequence}">[${sequenceSlice}]</small>`;
    }
    return "";
}

function processLibPrepKitRequired(changedSeqTypeRelatedElement, needLibPrepKit) {
    let libPrepKitInputs = changedSeqTypeRelatedElement.closest('tr').getElementsByClassName('libraryPreparationKit-dropdown');
    let visibleElementIndex = libPrepKitInputs.length > 1 ? 1 : 0;
    if (needLibPrepKit) {
        libPrepKitInputs[0].required = true;
        libPrepKitInputs[visibleElementIndex].classList.replace("validation-color-not-required", "validation-color");
    } else {
        libPrepKitInputs[0].required = false;
        libPrepKitInputs[visibleElementIndex].classList.replace("validation-color", "validation-color-not-required");
    }
}

function checkLaneNo() {
    let mapping = new Map();

    $(".lane-textfield:not([disabled])").each(function() {
        let currentLaneNo = $(this);
        let currentRunId = currentLaneNo.parents('tr').find('.runId-textfield');
        let currentCombi = currentLaneNo.val() + "-" + currentRunId.val();
        currentLaneNo[0].setCustomValidity("");
        currentRunId[0].setCustomValidity("");

        currentLaneNo.parents('td').find('.invalid-feedback .duplicated-run-id-lane-no').attr("hidden", true);
        currentRunId.parents('td').find('.invalid-feedback .duplicated-run-id-lane-no').attr("hidden", true);

        if (currentCombi !== '-') {
            if (mapping.get(currentCombi) == null) {
                let ids = new Set();
                ids.add(currentLaneNo.attr('id'));
                mapping.set(currentCombi, ids);
            } else {
                mapping.set(currentCombi, mapping.get(currentCombi).add(currentLaneNo.attr('id')));
            }
        }
    });

    for (let laneNoTextFieldIds of mapping.values()) {
        if (laneNoTextFieldIds.size > 1) {
            for (let laneNoId of laneNoTextFieldIds) {
                let laneInput = $('#'+laneNoId);
                laneInput[0].setCustomValidity("invalid");
                let extraInvalidFeedbackLane = laneInput.parents('td').find('.invalid-feedback .duplicated-run-id-lane-no');
                extraInvalidFeedbackLane.text("Duplicated combination of lane no and run id.");
                extraInvalidFeedbackLane.removeAttr('hidden');

                let runIdInput = $('#'+laneNoId.replace("lane", "runId"));
                runIdInput[0].setCustomValidity("invalid");
                let extraInvalidFeedbackRunId = runIdInput.parents('td').find('.invalid-feedback .duplicated-run-id-lane-no');
                extraInvalidFeedbackRunId.text("Duplicated combination of lane no and run id.");
                extraInvalidFeedbackRunId.removeAttr('hidden');
            }
        }
    }
}
