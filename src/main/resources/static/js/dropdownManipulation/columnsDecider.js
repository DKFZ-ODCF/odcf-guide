function decideToShowSeqTypeRelatedColumns() {
    decideToShowColumn('antibody-textfield', 'antibody');
    decideToShowColumn('antibodyTarget-dropdown', 'antibodyTarget');
    decideToShowColumn('antibodyTarget-dropdown', 'sampleTypeOnFilesystem');
    decideToShowColumn('tagmentationLibrary-textfield', 'tagmentationLibrary');
    decideToShowColumn('plate-textfield', 'plate');
    decideToShowColumn('wellPosition-textfield', 'wellPosition');
    decideToShowColumn('libraryPreparationKit-dropdown', 'libraryPreparationKit');
    decideToShowColumn('indexType-textfield', 'indexType');
    decideToShowColumn('lowCoverageRequested-checkbox', 'lowCoverageRequested');
    decideToShowColumn('sampleTypeCategory-dropdown', 'sampleTypeCategory');
}

function decideToShowColumn(lookupSelector, hideSelector) {
    if ($('.' + lookupSelector + ':enabled').length === 0) {
        $('.' + hideSelector).hide();
    } else {
        $('.' + hideSelector).show();
    }
    adjustScrollBars();
}

function adjustScrollBars() {
    let scrollTop = document.getElementById('scroll-container-top');
    let scrollBottom = document.getElementById('scroll-container-bottom');
    if (scrollTop != null) {
        scrollTop.style.width = scrollBottom.scrollWidth + "px";
    }
    if (scrollBottom != null) {
        scrollBottom.style.width = scrollBottom.scrollWidth + "px";
    }
}