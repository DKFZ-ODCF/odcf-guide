function processLowCoverageRequested(changedSeqTypeRelatedElement, lowCoverageRequested, runDecideToShow) {
    let lowCoverageRequestedCheckbox = changedSeqTypeRelatedElement.closest('tr')
        .getElementsByClassName('lowCoverageRequested-checkbox')[0];

    lowCoverageRequestedCheckbox.disabled = !lowCoverageRequested;
    if (runDecideToShow) {
        decideToShowColumn('lowCoverageRequested-checkbox', 'lowCoverageRequested');
    }
}
