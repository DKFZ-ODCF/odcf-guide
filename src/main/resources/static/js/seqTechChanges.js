function changeSeqTechData(id, name, importAlias, validationLevel) {
    document.getElementById('id').value = id;
    document.getElementById('name').value = name;
    document.getElementById('validationLevel').value = validationLevel;
    if (importAlias !== '[]') {
        let importAliases = importAlias.replace(/[\[\]]/g,'').split(",");
        for (let elem in importAliases) {
            document.getElementsByClassName('importAlias')[elem].value = importAliases[elem].trim();
            addImportAliases($(document.getElementsByClassName('btn-add')[0]));
        }
    } else {
        document.getElementById('importAlias').value = '';
    }
}
