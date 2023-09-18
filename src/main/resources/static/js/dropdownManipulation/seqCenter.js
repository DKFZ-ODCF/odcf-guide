function processCenterForIlseNumber(changedCenterRelatedElement, center) {
    var textfieldToggle = changedCenterRelatedElement.parents('tr').find('.externalSubmissionId-textfield');
    if (center === "DKFZ") {
        textfieldToggle.attr('disabled', false);
    } else {
        textfieldToggle.attr('disabled', true);
    }
}
