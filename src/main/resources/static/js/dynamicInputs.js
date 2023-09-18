$(document).ready(function() {
    setInputTrigger();
});

function setInputTrigger() {
    let inputs = document.getElementsByClassName('width-dynamic');
    for (let input of inputs) {
        input.addEventListener('input', resizeInput);
        resizeInput.call(input);
    }
}

function resizeInput() {
    this.style.width = `calc(${this.value.length}ch + 4ch + 1.75rem)`; // 1.75rem is the width of the dropdown icon
}

function hideSampleColumn(selector, buttonSelector) {
    $(selector).hide();
    $(buttonSelector).find('.button-icon').removeClass('fa-eye-slash').addClass('fa-eye');
    $(buttonSelector).attr('onclick', 'showSampleColumn("' + selector +'","' + buttonSelector + '")');
}

function showSampleColumn(selector, buttonSelector) {
    $(selector).show();
    $(buttonSelector).find('.button-icon').removeClass('fa-eye').addClass('fa-eye-slash');
    decideToShowSeqTypeRelatedColumns();
    $(buttonSelector).attr('onclick', 'hideSampleColumn("' + selector +'","' + buttonSelector + '")');
}
