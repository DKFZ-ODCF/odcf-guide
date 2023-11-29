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
    let parents = currentProjectSelect.parents('tr');
    let projectPrefixSpan = parents.find('.projectPrefix-span');
    if (newProjectPrefix) {
        let pidTextfield = parents.find('.pid-textfield');
        pidTextfield.val(pidTextfield.val().replace(new RegExp("^" + newProjectPrefix + "[-_]?"), ""));
        projectPrefixSpan.html(newProjectPrefix);
        let newPattern = pidTextfield[0].pattern.replace(new RegExp("[0-2],42"), Math.max(1, (3 - projectPrefixSpan.text().length)) + ",42");
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

function getSimilarPids(element) {
    let suggestions = element.closest('td').getElementsByClassName('pid-suggestions')[0];
    let suggestionsLabel = element.closest('td').getElementsByClassName('pid-suggestions-label')[0];
    let suggestionsList = suggestions.getElementsByTagName("ul")[0];
    suggestionsList.innerHTML = "";
    if (element.checkValidity() && element.value.length > 2) {
        let prefix = element.parentElement.getElementsByClassName('projectPrefix-span')[0].innerText;
        let project = element.closest('tr').getElementsByClassName('project-dropdown')[0].value;
        let request = new XMLHttpRequest();
        request.open("GET", `/get-similar-pids?pid=${element.value}&project=${project}`, true);
        request.onreadystatechange = function () {
            if (request.readyState === 4 && request.status === 200 && request.responseText !== "") {
                let elems = request.responseText.split(',');
                elems.forEach(option => {
                    let li = document.createElement("li");
                    li.textContent = option;
                    li.addEventListener("click", function closeLi() {
                        element.value = option.replace(prefix, "");
                        resizeInput.call(element);
                        handleStyleDisplay(suggestions, suggestionsLabel, "none");
                        li.removeEventListener('click', closeLi);
                    });
                    suggestionsList.appendChild(li);
                });
                if (suggestionsList.children.length > 0) {
                    handleStyleDisplay(suggestions, suggestionsLabel, "block");
                } else {
                    handleStyleDisplay(suggestions, suggestionsLabel, "none");
                }
            } else {
                handleStyleDisplay(suggestions, suggestionsLabel, "none");
            }
        };
        request.send(null);
    } else if (element.checkValidity()) {
        let li = document.createElement("li");
        li.textContent = "Please enter at least 3 characters";
        suggestionsList.appendChild(li);
        handleStyleDisplay(suggestions, suggestionsLabel, "block");
    } else {
        handleStyleDisplay(suggestions, suggestionsLabel, "none");
    }

    document.addEventListener("click", function closeDocument(event) {
        if (!suggestions.contains(event.target) && element !== event.target) {
            handleStyleDisplay(suggestions, suggestionsLabel, "none");
            document.removeEventListener('click', closeDocument);
        }
    });
}

function handleStyleDisplay(suggestions, suggestionsLabel, value) {
    suggestions.style.display = value;
    suggestionsLabel.style.display = value;
}
