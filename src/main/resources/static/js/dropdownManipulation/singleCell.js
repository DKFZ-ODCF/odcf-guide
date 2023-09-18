function performSingleCellWellLabelUpdate(changedSeqTypeRelatedElement, isSingleCell, seqType, runDecideToShow) {
    let row = changedSeqTypeRelatedElement.closest('tr');

    let singleCellWellLabelToDisable  = !isSingleCell || seqType.startsWith('10x') || seqType === '-- select --';
    row.getElementsByClassName('plate-textfield')[0].disabled = singleCellWellLabelToDisable;
    row.getElementsByClassName('wellPosition-textfield')[0].disabled = singleCellWellLabelToDisable;
    if (runDecideToShow) {
        decideToShowColumn('plate-textfield', 'plate');
        decideToShowColumn('wellPosition-textfield', 'wellPosition');
    }
}
