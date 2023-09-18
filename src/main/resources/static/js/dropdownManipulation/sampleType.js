function sampleTypeOnChangeEvents(changedSampleTypeTextField) {
    performSampleTypeRegexCheck(changedSampleTypeTextField);
    changeSampleName(changedSampleTypeTextField);
    updateSampleTypePrefix(changedSampleTypeTextField);
    triggerOnchangeSpeciesByOtherField(changedSampleTypeTextField[0]);
    checkSampleTypeCategory(changedSampleTypeTextField[0]);
}

function performSampleTypeRegexCheck(changedSampleTypeTextfield) {
    var reg_forbiddenXSuffix = RegExp('.+?-x$', "i");

    var sampleType = changedSampleTypeTextfield.val();
    if (reg_forbiddenXSuffix.test(sampleType)) {
        changedSampleTypeTextfield.val(sampleType.substr(0, sampleType.length-2));
        alert("Sample type must not end with '-x'!\nYour input has been modified to meet requirements!");
    }
    validateMetadataTableForm(false);
}

function processSampleTypesUpdate(currentProjectSelect, newContent) {
    var pattern_numberSuffix = $('#regexSampleType').text();
    var regex = RegExp(pattern_numberSuffix);
    var pattern_noNumberSuffix = $('#regexOldSampleType').text();
    var errorOldSampleType = $('#detailsValidationErrorOldSampleType').html();
    var errorSampleType = $('#detailsValidationErrorSampleType').html();

    var row = currentProjectSelect.parents('tr');
    var targetDropdown = row.find('.sampleType-dropdown');
    var targetInput = row.find('.sampleType-textfield');
    var invalidFeedback = row.find('.sampleType .invalid-feedback span');
    var oldValue = targetDropdown.val();
    var selectedCode = newContent.includes(oldValue) || false ? '' : 'selected="selected"';
    var newOptions = '<option disabled="disabled" '+selectedCode+'>-- select sample type --</option>';
    var foundStWithoutNumberSuffix = false;
    for ( var i = 0; i < newContent.length; i++) {
        selectedCode = oldValue === newContent[i] ? 'selected="selected"' : '';
        newOptions += '<option value="' + newContent[i] + '" '+selectedCode+'>' + newContent[i] + '</option>';
        if (!regex.test(newContent[i])) {
            targetInput.attr('pattern', pattern_noNumberSuffix);
            targetInput.attr('title', errorOldSampleType);
            invalidFeedback.html(errorOldSampleType);
            foundStWithoutNumberSuffix = true;
        } else if (!foundStWithoutNumberSuffix) { // do re-add removed constraint
            targetInput.attr('pattern', pattern_numberSuffix);
            targetInput.attr('title', errorSampleType);
            invalidFeedback.html(errorSampleType);
        }
    }
    targetDropdown.html(newOptions);
}

async function performSampleTypesUpdateForCurrentProject(currentProjectSelect) {
    var sampleTypes = await getContentForCurrentProject(currentProjectSelect, 'projectSampleTypesMapping', 'sample-types');
    processSampleTypesUpdate(currentProjectSelect, sampleTypes);
}

function updateSampleTypePrefix(element) {
    var prefix = element.parents('tr').find('.sampleTypeOnFilesystemPrefix-span');
    prefix.text(element.val());
}

function processSampleTypeCategory(changedSeqTypeRelatedElement, needSampleTypeCategory, runDecideToShow) {
    let sampleTypeCategoryDropdown = changedSeqTypeRelatedElement.closest('tr').getElementsByClassName('sampleTypeCategory-dropdown')[0];

    if (needSampleTypeCategory) {
        checkSampleTypeCategory(sampleTypeCategoryDropdown);
        sampleTypeCategoryDropdown.selectize.enable();
    } else {
        sampleTypeCategoryDropdown.selectize.disable();
    }

    if (runDecideToShow) {
        decideToShowColumn('sampleTypeCategory-dropdown', 'sampleTypeCategory');
    }
}

function checkSampleTypeCategory(changedRelatedElement) {
    let sampleTypeCategoryDropdown = changedRelatedElement.closest('tr').getElementsByClassName('sampleTypeCategory-dropdown')[0];
    let project = sampleTypeCategoryDropdown.closest('tr').getElementsByClassName('project-dropdown')[0].value;
    let sampleType = sampleTypeCategoryDropdown.closest('tr').getElementsByClassName('sampleType-textfield')[0].value.toLowerCase();

    if (project !== "" && sampleType !== "") {
        let methodName = "sample-type-category-by-project-and-sample-type";
        let url = encodeURI(`/get-value-from-external-metadata-source/${methodName}?project=${project}&sampleType=${sampleType}`);
        const xhttp = new XMLHttpRequest();
        xhttp.open("GET", url);
        xhttp.send();
        xhttp.onload = function() {
            let sampleTypeCategory = this.response;
            if (sampleTypeCategory  === "") {
                sampleTypeCategory = "UNDEFINED";
            }
            sampleTypeCategoryDropdown.selectize.addItem(sampleTypeCategory, true);
            // this makes the sampleTypeCategoryDropdown read-only if the sampleTypeCategory is TUMOR or CONTROL
            if (!(["IGNORED", "UNDEFINED"].includes(sampleTypeCategory))) {
                sampleTypeCategoryDropdown.selectize.lock();
            } else {
                sampleTypeCategoryDropdown.selectize.unlock();
            }
        };
    }

}
