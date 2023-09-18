/**
 * OVERRIDE:
 * Hides the input element out of view, while
 * retaining its focus.
 */
Selectize.prototype.hideInput = function() {
    var self = this;

    self.setTextboxValue('');
    self.$control_input.css({opacity: 0});
    self.isInputHidden = true;
    return self;
};

/**
 * OVERRIDE:
 * Restores input visibility.
 */
Selectize.prototype.showInput = function() {
    var self = this;
    self.$control_input.css({opacity: 1});
    self.isInputHidden = false;
    return self;
};

/**
 * Create all selectize inputs from plain HTML objects.
 */
function initSelectizeSpecies() {
    let sessionStorageMap = getMapFromSessionStorage("requestedValues");

    let dropdowns = document.querySelectorAll('select.selectize-species');
    let selectizedDropdowns = [];
    for (let dropdown of dropdowns) {
        if (dropdown.selectize === undefined && !dropdown.closest('table').hidden) {
            $(dropdown).selectize({
                plugins: ["drag_drop", "remove_button", "restore_on_backspace"],
                delimiter: "+",
                create: function (newSpecies) {
                    let newSpeciesTemp = newSpecies.replaceAll("(ReqVal)", "") + "(ReqVal)";
                    processNewSpeciesSimilarity(newSpeciesTemp, this);
                    return {value: newSpeciesTemp, text: newSpeciesTemp };
                },
                persist: true,
                closeAfterSelect: true,
                dropdownParent: 'body',
                maxItems: 2,
                openOnFocus: false,
                createOnBlur: true,
                onInitialize: function () {
                    this.clear(true);
                    let items = this.$input.data("items")?.split('+');
                    if (items === undefined || items[0] === "" || items[0] === "empty") {
                        this.$control[0].setAttribute('data-custom-valid', 'false');
                    } else {
                        for (let i = 0; i < items.length; i++) {
                            let foundOption = Object.entries(this.options).find(option => option[1].text === items[i] + '(ReqVal)' || option[1].text === items[i]);
                            if (foundOption !== undefined) {
                                this.addItem(foundOption[0], true);
                            } else {
                                let fieldName = "speciesWithStrain";
                                let fieldValues = new Set(sessionStorageMap.get(fieldName)?.split("; "));
                                if (fieldValues === undefined) { fieldValues = new Set(); }
                                fieldValues.add(items[i]);
                                sessionStorageMap.set(fieldName, [...fieldValues].join("; "));

                                /*let value = items[i] + "(ReqVal)";
                                this.addOption({ value: value , text: value });
                                this.addItem(value, true);
                                processNewValueRequest(value, 'select.selectize-species.selectized');*/
                            }
                        }
                    }
                },
                render: {
                    item: function (item, escape) {
                        return "<div class='item'>" +
                            (item.text ? '<span class="name">' + escape(item.text.replace('(ReqVal)', '')) + "</span>" : "") +
                            (item.value.endsWith('(ReqVal)') ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option: function (item, escape) {
                        return "<div class='option'>" +
                            (item.text ? '<span class="name">' + escape(item.text.replace('(ReqVal)', '')) + "</span>" : "") +
                            (item.value.endsWith('(ReqVal)') ? '&nbsp;<i class="fas fa-hourglass-half" title="Approval pending"></i>' : '') +
                            "</div>";
                    },
                    option_create: function(data, escape) {
                        return '<div class="create">Request: <strong>' + escape(data.input) + '</strong></div>';
                    }
                },
            });
            if (!dropdown.classList.contains('species-bulk-edit')) {
                selectizedDropdowns.push(dropdown);
            }
        }
    }
    selectizedDropdowns.forEach(elem => elem.onchange());
    sessionStorage.setItem("requestedValues", JSON.stringify(Array.from(sessionStorageMap.entries())));
}

/**
 * Requests species from otp, sets custom validation attributes and triggers form validation.
 * Run {@link validateMetadataTableForm} afterwards.
 * @param {object} element species dropdown
 */
async function onchangeSpecies(element) {
    let selectInput = element.parentElement.getElementsByClassName('selectize-input')[0];
    if (!selectInput.classList.contains("required")) {
        selectInput.setAttribute('data-custom-valid', 'true');
        validateMetadataTableForm(false);
        return;
    }
    if (element.options.length === 0) {
        selectInput.setAttribute('data-custom-valid', 'false');
        validateMetadataTableForm(false);
        return;
    }

    let row = element.closest('tr');
    selfCheck(row.closest('table').getElementsByClassName('sampleRow'));

    let pidInput = row.getElementsByClassName('pid-textfield')[0].value;
    let prefix = row.getElementsByClassName('projectPrefix-span')[0].innerText;
    let pid = prefix + pidInput;
    let sampleTypeInput = row.getElementsByClassName('sampleType-textfield')[0].value;
    let xenograftSuffix = row.getElementsByClassName('xenograftCheckbox')[0].checked ? '-x' : '';
    let sampleType = sampleTypeInput + xenograftSuffix;
    let project = row.getElementsByClassName('project-dropdown')[0].value;
    let allSpeciesGuide = Array.from(element.options).map(e => e.value);

    const xhttp = new XMLHttpRequest();
    let url = encodeURI(`/get-species-otp-validity?pid=${pid}&sampleType=${sampleType}&project=${project}&species=${allSpeciesGuide}`);
    xhttp.open("GET", url);
    xhttp.send();
    xhttp.onload = function() {
        selectInput.setAttribute('data-custom-valid-external', this.response);
        selectInput.setAttribute('data-custom-valid', (this.response === 'true' &&
            selectInput.dataset.customValidSelf === 'true').toString());
        validateMetadataTableForm(false);
    };

}

/**
 * Trigger onchange event on corresponding species dropdown if other input was changed.
 * @param {object} element input field
 */
async function triggerOnchangeSpeciesByOtherField(element) {
    element.closest('tr').getElementsByClassName('species-dropdown')[0].onchange();
}

/**
 * Self check of inconsistencies in species input in all rows.
 * Run {@link validateMetadataTableForm} afterwards.
 * @param {HTMLCollectionOf<object>} rows table rows
 */
function selfCheck (rows) {
    let rowsGroupedByPidAndSampleType = new Map();
    for (let row of rows) {
        let selectInput = row.querySelector('.selectize-species .selectize-input');
        if (selectInput !== undefined) {
            let pidInput = row.getElementsByClassName('pid-textfield')[0].value;
            let prefix = row.getElementsByClassName('projectPrefix-span')[0].innerText;
            let sampleType = row.getElementsByClassName('sampleType-textfield')[0].value;
            let xenograftSuffix = row.getElementsByClassName('xenograftCheckbox')[0].checked ? '-x' : '';

            let key = `${prefix}${pidInput}:${sampleType}${xenograftSuffix}`;
            let rows = rowsGroupedByPidAndSampleType.get(key);
            if (rows === undefined) {
                rows = new Array(row);
            } else {
                rows.push(row);
            }
            rowsGroupedByPidAndSampleType.set(key, rows);
        }
    }
    for (let [key, rows] of rowsGroupedByPidAndSampleType) {
        let validPidAndSampleType = new Set(
            Array.from(rows)
                .map(row =>
                    Array.from(row.getElementsByClassName('species-dropdown')[0].options)
                        .map(option => option.value)
                        .join()
                )
        ).size === 1;
        for (let row of rows) {
            let selectInput = row.querySelector('.selectize-species .selectize-input');
            let dropdown = row.getElementsByClassName('species-dropdown')[0];
            let valid = validPidAndSampleType && !(row.getElementsByClassName('xenograftCheckbox')[0].checked && dropdown.options.length < 2);
            selectInput.setAttribute('data-custom-valid-self', valid.toString());
            selectInput.setAttribute('data-custom-valid', (valid && selectInput.dataset.customValidExternal === 'true').toString());
        }
    }
    validateMetadataTableForm(false);
}

/**
 * Checks whether a similar species already exists and displays it to the user.
 * @param {string} newSpecies
 * @param {Object} selectizeObject
 */
function processNewSpeciesSimilarity(newSpecies, selectizeObject) {
    let selector = 'select.selectize-species.selectized';

    $.getJSON("/new-species-similarity-check", {
        newSpecies : newSpecies,
        ajax : 'true'
    }, function(resultJson) {
        postProcessNewValueSimilarity(resultJson, newSpecies, selector, selectizeObject);
    });
}
