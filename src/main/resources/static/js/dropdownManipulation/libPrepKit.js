function processLibPrepKitRequired(changedSeqTypeRelatedElement, needLibPrepKit) {
    let libPrepKitInputs = changedSeqTypeRelatedElement.closest('tr').getElementsByClassName('libraryPreparationKit-dropdown');
    if (needLibPrepKit) {
        libPrepKitInputs[0].required = true;
        libPrepKitInputs[1].classList.replace("validation-color-not-required", "validation-color");
    } else {
        libPrepKitInputs[0].required = false;
        libPrepKitInputs[1].classList.replace("validation-color", "validation-color-not-required");
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
