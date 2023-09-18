$(document).ready(function(){
    $('#deleteParserModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var parserProject = button.data('whatever');
        $('#deleteParserOfProject').text('Delete parser of project "' + parserProject + '"?');
        let url = "/parser/deleteParser?project=" + parserProject;
        $('#deleteParserUrl').attr("href", url);
    });
});

function addParserMapping(addButton, fieldName) {
    let entry = addButton.closest('.entry');
    let wrapper =  addButton.closest('.' + fieldName + 'Mapping');
    let newEntry = entry.clone();

    newEntry.addClass("mt-2");
    newEntry.find('input').val('');
    let key = newEntry.find('.' + fieldName + 'MappingKey');
    let index = parseInt(key.attr('name').match(/parserMappingKeys\[([0-9]+)\]/)[1]) + 1;
    key.attr('name', key.attr('name').replace(/parserMappingKeys\[[0-9]+\]/g, 'parserMappingKeys[' + index + ']'));
    let value = newEntry.find('.' + fieldName + 'MappingValue');
    value.attr('name', value.attr('name').replace(/parserMappingValues\[[0-9]+\]/g, 'parserMappingValues[' + index + ']'));
    wrapper.append(newEntry);

    addButton.removeClass('btn-add').addClass('btn-remove')
        .removeClass('btn-success').addClass('btn-danger')
        .attr('onclick', 'removeParserMapping($(this))')
        .html('<i class="fas fa-minus"></i>');
}

function removeParserMapping(removeButton) {
    removeButton.closest('.entry').remove();
    return false;
}


function addParserComponent(addButton) {
    let card = addButton.closest('.card');
    let cardDeck = card.closest('.card-deck');
    let newCard = card.clone();

    newCard.addClass("mx-2");
    let inputs = newCard.find('input');
    inputs.not('.numberField').val('');
    inputs.filter('.numberField').val(0);
    inputs.each(function () {
        let oldName = $(this).attr('name');
        let index = parseInt(oldName.match(/.*parserComponents\[([0-9]+)\].*/)[1]) + 1;
        $(this).attr('name', oldName.replace(/(.*parserComponents\[)[0-9]+(\].*)/, "$1" + index + "$2"));
    });
    newCard.find('[old-value]').attr('old-value', 'trick17');
    cardDeck.append(newCard);

    addButton.hide();
}

function removeParserComponent(removeButton, parserField) {
    let fieldRegexInput = $('#' + parserField + 'Regex');
    let fieldOrderInput = $('#' + parserField + 'Order');
    let card = removeButton.closest('.card');
    let cardDeck = card.closest('.card-deck');

    if (cardDeck.find('.card').length === 1) {
        return;
    }
    let re = new RegExp("(.*)\\[" + card.find('[old-value]').val() + "\\](.*)");
    fieldRegexInput.val(fieldRegexInput.val().replace(re, "$1$2"));
    fieldOrderInput.val(fieldOrderInput.val().replace(re, "$1$2"));
    card.remove();

    cardDeck.find('.card:last').find('.componentButton.btn-add').show();
}

function updateComponentName(componentNameField, fieldName) {
    let fieldRegexInput = $('#' + fieldName + 'Regex');
    let fieldOrderInput = $('#' + fieldName + 'Order');
    let oldValue = componentNameField.attr('old-value');
    let newValue = componentNameField.val();
    if (oldValue === "trick17") {
        oldValue = "";
    }
    if (oldValue === "") {
        fieldRegexInput.val(fieldRegexInput.val() + '[' + newValue + ']');
        fieldOrderInput.val(fieldOrderInput.val() + '[' + newValue + ']');
    } else {
        let re = new RegExp("(.*\\[)" + oldValue + "(\\].*)");
        fieldRegexInput.val(fieldRegexInput.val().replace(re, "$1" + newValue + "$2"));
        fieldOrderInput.val(fieldOrderInput.val().replace(re, "$1" + newValue + "$2"));
    }
    componentNameField.attr('old-value', newValue);
}

function updateWholeRegex() {
    $('#parserRegex').removeAttr("readonly");
}

function updateParserCheck() {
    document.getElementById("parserRegexNew").innerHTML = document.getElementById("parserRegex").value;
    document.getElementById("parserRegexOld").innerHTML = document.getElementById("currentRegex").innerText;

    if(document.getElementById("patient_idRegex")) {
        document.getElementById("PidFRegexNew").innerHTML = document.getElementById("patient_idRegex").value;
        document.getElementById("PidFRegexOld").innerHTML = document.getElementById("currentpatient_idFRegex").innerText;
    }
    if(document.getElementById("sample_typeRegex")) {
        document.getElementById("SampleFRegexNew").innerHTML = document.getElementById("sample_typeRegex").value;
        document.getElementById("SampleFRegexOld").innerHTML = document.getElementById("currentsample_typeFRegex").innerText;
    } else {
        let sampleRows = document.getElementsByClassName("sampleRow");
        for (let i = 0; i < sampleRows.length; i++) {
            sampleRows[i].setAttribute("hidden", "true");
        }
    }
}

