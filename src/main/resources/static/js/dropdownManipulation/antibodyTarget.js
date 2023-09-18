function processAntibodyTargetDropdownStatus(changedSeqTypeRelatedElement, hasAntibodyTarget, runDecideToShow) {
    let row = changedSeqTypeRelatedElement.closest('tr');
    var antibodyDropdown = row.getElementsByClassName('antibodyTarget-dropdown')[0];
    var antibodyTextfield = row.getElementsByClassName('antibody-textfield')[0];
    var sampleTypeOnFilesystem = row.getElementsByClassName('sampleTypeOnFilesystem-span')[0];
    antibodyDropdown.required = hasAntibodyTarget;
    antibodyDropdown.disabled = !hasAntibodyTarget;
    if (hasAntibodyTarget) {
        antibodyDropdown.selectize.enable();
        sampleTypeOnFilesystem.style.display = 'block';
    } else {
        antibodyDropdown.selectize.disable();
        sampleTypeOnFilesystem.style.display = 'none';
    }
    antibodyTextfield.disabled = !hasAntibodyTarget;
    if (runDecideToShow) {
        decideToShowColumn('antibody-textfield', 'antibody');
        decideToShowColumn('antibodyTarget-dropdown', 'antibodyTarget');
        decideToShowColumn('antibodyTarget-dropdown', 'sampleTypeOnFilesystem');
    }

    updateSampleTypeSuffix(antibodyDropdown);
}

function updateSampleTypeSuffix(element) {
    let sampleTypeSuffix = element.closest('tr').getElementsByClassName('sampleTypeOnFilesystemSuffix-span')[0];
    sampleTypeSuffix.innerHTML = element.value.replace("(ReqVal)", "");
}
