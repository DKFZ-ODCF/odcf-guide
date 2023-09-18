function addImportAliases(addButton) {
    var dynaForm = $('.dynamic-wrap .form-group:first'),
        currentEntry = addButton.parents('.entry:first'),
        newEntry = $(currentEntry.clone().addClass("mt-2")).appendTo(dynaForm);

    newEntry.find('input').val('');
    dynaForm.find('.entry:not(:last) .btn-add')
        .removeClass('btn-add').addClass('btn-remove')
        .removeClass('btn-success').addClass('btn-danger')
        .attr('onclick', 'removeImportAliases($(this))')
        .html('<i class="fas fa-minus"></i>');
}

function removeImportAliases(removeButton) {
    removeButton.parents('.entry:first').remove();
    return false;
}
