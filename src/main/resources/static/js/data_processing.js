
function turnOffFieldConstraints(disableNext = true) {
    $('.nextButton').prop('disabled', disableNext).attr('data-original-title', 'This button is currently disabled. Please use Quick Save to continue.');
    let form = $('#metadataTableForm')[0];
    form.classList.add('no-validation');
    form.classList.remove('needs-validation');
    let button = document.getElementsByClassName("regex-button")[0];
    if (button) {
        button.innerHTML = "NO field constraints!";
        button.disabled = true;
        button.classList.remove("btn-outline-danger");
        button.classList.add("btn-danger");
    }
    $('<input type="hidden" name="noFieldConstraints" value="true">').appendTo($("#metadataTableForm"));
}

function toggleNonProceedRows() {
    var buttons = $("button[name='toggleRows']");
    if (buttons.find(".fa-eye").length > 0) {
        $(".samplesTable.d-none").removeClass("d-none").addClass("d-table-row");
        buttons.find(".fa-eye").removeClass("fa-eye").addClass("fa-eye-slash");
        buttons.find("#hideRowsText").show();
        buttons.find("#showRowsText").hide();
    } else {
        $(".samplesTable.d-table-row").removeClass("d-table-row").addClass("d-none");
        buttons.find(".fa-eye-slash").removeClass("fa-eye-slash").addClass("fa-eye");
        buttons.find("#hideRowsText").hide();
        buttons.find("#showRowsText").show();
    }
}

function save_without_validation(valueText) {
    turnOffFieldConstraints();
    $("#metadataTableForm").on("submit", function () {
        $('<input type="hidden" name="button" value="' + valueText + '">').appendTo($(this));
    });
    $('#metadataTableForm').submit();
}

function confirm_submission_deletion(ilse) {
    return confirm("DELETE submission "+ilse+"?");
}


function reset_submission(identifier, uuid) {
    if (confirm("Reset submission " + identifier + "?\n\nAll adaptations you made so far will be removed.")) {
        var form = document.createElement("form");
        document.body.appendChild(form);
        form.method = "POST";
        form.action = "/metadata-validator/submission-actions/reset";
        var submission = document.createElement("input");
        submission.name = "submissionUuid";
        submission.value = uuid;
        submission.type = "hidden";
        form.appendChild(submission);
        form.submit();
    }
}
