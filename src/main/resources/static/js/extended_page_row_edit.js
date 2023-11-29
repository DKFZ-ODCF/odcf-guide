function newRow() {
    let table = document.getElementById('csv-details-table');
    let newRow = document.getElementById('clone-row').cloneNode(true);
    newRow.removeAttribute('id');
    let indexes = table.querySelector("tbody:last-child [name^='sampleList']").name.match(/\d+/);
    let sampleIndex = parseInt(indexes[0]) + 1;
    newRow.querySelectorAll("[name^='sampleList']").forEach(function (element) {
        let oldName = element.name.match(/([a-zA-Z]+\[)\d+(.+)/);
        element.name = oldName[1] + sampleIndex + oldName[2];
    });
    table.appendChild(newRow);
    setInputTrigger();
    fixTooltip();
    initSelectizeProject();
    initSelectizeWithRequestOption();
    initSelectizeGeneral();
    initSelectizeLibPrepKit();
    initSelectizeSeqType();
    initSelectizeSpecies();
}

function newFileRow(button) {
    let tbody = button.closest('tbody');
    let newRow = document.getElementById('clone-file-row').cloneNode(true);
    newRow.removeAttribute('id');
    let indexes = tbody.querySelector("tr:last-child [name*='files']").name.match(/sampleList\[(\d+)]\.files\[(\d+).*/);
    let sampleIndex = parseInt(indexes[1]);
    let fileIndex = parseInt(indexes[2]) + 1;
    newRow.querySelectorAll("[name^='sampleList']").forEach(function (element) {
        let oldName = element.name.match(/(sampleList\[)\d+(]\.files\[)\d+(.*)/);
        element.name = oldName[1] + sampleIndex + oldName[2] + fileIndex + oldName[3];
    });
    tbody.appendChild(newRow);
    setRowSpan(tbody);
    setInputTrigger();
    fixTooltip();
}

function deleteRow(button) {
    let row = button.closest('tr');
    let tbody = row.closest('tbody');
    if (tbody.querySelectorAll('tr').length > 1) {
        let sampleInfos = row.querySelectorAll("td[rowspan]");
        if (sampleInfos.length > 0) {
            let newRow = tbody.children[1];
            sampleInfos.forEach( element => {
                let index = element.cellIndex;
                newRow.insertChildAtIndex(element, index);
                row.insertChildAtIndex(element.cloneNode(), index); //placeholder for index
            });
            let hidden = row.querySelectorAll("tr > [type=hidden]:not([name*='files'])");
            hidden.forEach( element => newRow.insertChildAtIndex(element, element.cellIndex));
        }
        row.remove();
        setRowSpan(tbody);
        setInputTrigger();
    } else {
        tbody.remove();
    }
    fixTooltip();
}

function setRowSpan(tbody) {
    let rows = tbody.getElementsByTagName("tr");
    tbody.querySelectorAll("[rowspan]").forEach(element => element.rowSpan = rows.length);
    rows[0].classList.add("sampleRow");
}

/* fix tooltip bug(s) from boostrap. */
function fixTooltip() {
    const elements = document.getElementsByClassName('tooltip');
    while (elements.length > 0){
        elements[0].parentNode.removeChild(elements[0]);
    }
    $('[data-toggle="tooltip"]').tooltip({trigger : 'hover'});
}

function toggleRowButtons() {
    let buttons = document.getElementsByClassName('row-edit-button');
    let newState = (buttons[0].style.display === 'none' || buttons[0].style.display === '') ? 'table-cell' : 'none';
    for (let item of buttons) {
        item.style.display = newState;
    }
    let icon = document.getElementById('edit-rows-icon');
    if (newState === 'none') {
        icon.className = 'fas fa-eye';
    } else {
        icon.className = 'fas fa-eye-slash';
    }
}
