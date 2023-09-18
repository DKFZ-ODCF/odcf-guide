function processTagmentationLibraryRequired(changedSeqTypeRelatedElement, tagmentation, runDecideToShow) {
    let tagmentationLibraryInput = changedSeqTypeRelatedElement.closest('tr')
        .getElementsByClassName('tagmentationLibrary-textfield')[0];

    tagmentationLibraryInput.disabled = !tagmentation;
    tagmentationLibraryInput.required = tagmentation;
    if (runDecideToShow) {
        decideToShowColumn('tagmentationLibrary-textfield', 'tagmentationLibrary');
    }
}
