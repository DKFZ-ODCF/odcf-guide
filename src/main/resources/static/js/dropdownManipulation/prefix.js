/**
 * Create all project selectize inputs from plain HTML objects.
 */
function initSelectizeProject() {
    let dropdowns = document.querySelectorAll('select.selectize-project');
    for (let dropdown of dropdowns) {
        if (dropdown.selectize === undefined && !dropdown.closest('table').hidden) {
            $(dropdown).selectize({
                create: false,
                persist: false,
                closeAfterSelect: true,
                dropdownParent: 'body',
                respect_word_boundaries: false,
                render: {
                    item: function (item, escape) {
                        return "<div>" +
                            (item.text ? '<span class="name">' + escape(item.value) + "</span>" : "") +
                            (item.text.endsWith('(closed)') ? '&nbsp;<small class="text-muted align-text-top">[closed]</small>' : '') +
                            "</div>";
                    },
                    option: function (item, escape) {
                        return "<div class='option'>" +
                            (item.text ? '<span class="name">' + escape(item.value) + "</span>" : "") +
                            (item.text.endsWith('(closed)') ? '&nbsp;<small class="text-muted align-text-top">[closed]</small>' : '') +
                            "</div>";
                    },
                },
            });
        }
    }
}

function processProjectPrefixUpdate(currentProjectSelect, newProjectPrefix) {
    var parents = currentProjectSelect.parents('tr');
    var projectPrefixSpan = parents.find('.projectPrefix-span');
    if (newProjectPrefix) {
        var pidTextfield = parents.find('.pid-textfield');
        pidTextfield.val(pidTextfield.val().replace(new RegExp("^" + newProjectPrefix + "[-_]?"), ""));
        projectPrefixSpan.html(newProjectPrefix);
        var newPattern = "^[A-Za-z0-9_+-]{" + Math.max(1, (3 - projectPrefixSpan.text().length)) + ",42}$";
        pidTextfield.attr('pattern', newPattern);
    } else {
        projectPrefixSpan.html('');
    }
}

async function performProjectPrefixContentUpdateForCurrentProject(currentProjectSelect) {
    if (currentProjectSelect.val() !== "") {
        let projectPrefix = await getContentForCurrentProject(currentProjectSelect, 'projectPrefixMapping', 'project-prefix');
        processProjectPrefixUpdate(currentProjectSelect, projectPrefix[0]);
    }
}
