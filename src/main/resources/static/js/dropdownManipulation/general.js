document.addEventListener("DOMContentLoaded", function(event) {
    var readyStateCheckInterval = setInterval(function() {
        if (document.readyState === "complete") {
            clearInterval(readyStateCheckInterval);
            document.querySelectorAll('[pattern=""]').forEach(element => element.removeAttribute('pattern'));
            document.getElementById('table-loader').style.display = "none";
            document.getElementById('loaded-content').style.display = "block";
            let width = window.getComputedStyle(document.querySelector('.tblnavigate'))
                .getPropertyValue("width");
            document.getElementById('scroll-container-top').style.width = width;
            document.getElementById('scroll-container-bottom').style.width = width;
        }
    }, 10);

    $('input[type="text"]').change(function(){
        this.value = $.trim(this.value);
    });

    initialize();

    document.querySelector(".wrapper-top").addEventListener("scroll", function() {
        adjustScrollBars();
        document.querySelector(".wrapper-bottom").scrollLeft = this.scrollLeft;
    });
    document.querySelector(".wrapper-bottom").addEventListener("scroll", function() {
        adjustScrollBars();
        document.querySelector(".wrapper-top").scrollLeft = this.scrollLeft;
    });

    let unknownRequestedValues = getMapFromSessionStorage("requestedValues");
    if (unknownRequestedValues !== null && unknownRequestedValues?.size !== 0) {
        $('#importWithUnknownValuesFoundModal').modal('show');
        let text = "";
        for (let [key, value] of unknownRequestedValues) {
            let readableName = key.replace(/([A-Z])/g, ' $1').replace(/^./, function(str){ return str.toUpperCase(); });
            let formattedValues = "";
            value.split("; ").forEach( element => formattedValues += `<dd class="mb-0 ml-3">- ${element}</dd>` );

            text += `<dt>${readableName}:</dt> ${formattedValues}`;
        }
        let temp = document.getElementById("importWithNewValuesText");
        temp.innerHTML = temp.innerHTML.replace("{0}", `<dl>${text}</dl>`);
    }

});

function Sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function initialize() {
    initSelectizeProject();
    initSelectizeWithRequestOption();
    initSelectizeGeneral();
    initSelectizeSeqType();
    $('select.project-dropdown').each(function() {
        performSampleTypesUpdateForCurrentProject($(this));
        performProjectPrefixContentUpdateForCurrentProject($(this));
    });
    initSelectizeSpecies();
    decideToShowSeqTypeRelatedColumns();
}

/**
 * Create all selectize inputs from plain HTML objects.
 */
function initSelectizeGeneral() {
    let dropdowns = document.querySelectorAll('select.selectize-general');
    for (let dropdown of dropdowns) {
        if (dropdown.selectize === undefined && !dropdown.closest('table').hidden) {
            $(dropdown).selectize({
                create: false,
                persist: false,
                closeAfterSelect: true,
                dropdownParent: 'body',
            });
        }
    }
}

function projectChanger(currentProjectSelect) {
    performSampleTypesUpdateForCurrentProject(currentProjectSelect);
    performProjectPrefixContentUpdateForCurrentProject(currentProjectSelect);
    performProjectLinkUpdateForCurrentProject(currentProjectSelect);
    checkSampleTypeCategory(currentProjectSelect[0]);
    if (document.getElementById('applyParser').hasAttribute("hidden")) {
        performShowParserCheck(currentProjectSelect);
    }
}

async function performShowParserCheck(currentProjectSelect) {
    await $.getJSON("/get-parser-availability-for-project", {
        projectName : currentProjectSelect.val(),
        ajax : 'true'
    }, function(result) {
        if (result) {
            document.getElementById('applyParser').removeAttribute('hidden');
        }
    });
}

function performProjectLinkUpdateForCurrentProject(currentProjectSelect) {
    $('.link-container').removeAttr('hidden');
    if (document.getElementById('project-link-container-empty') != null) {
        document.getElementById('project-link-container-empty').hidden = true;
    }

    var otpLinks = $('.otp-links');
    var otpProjectConfig = $('.otp-project-config');
    var currentProject = currentProjectSelect.val();
    var mustInsert = true;

    let projects = new Set;
    $.each($('.selectize-project.project-dropdown.selectized'), function(index, value) {
        if (value.value !== "") projects.add(value.value);
    });

    $.each(otpLinks, function(index, value) {
        if (value.text === currentProject) {
            mustInsert = false;
        }
        if (!projects.has(value.text)) value.remove();
    });

    $.each(otpProjectConfig, function(index, value) {
        if (!projects.has(value.text)) value.remove();
    });

    if (mustInsert) {
        var newProjectA = $('#otp-links-hidden').clone().appendTo('.link-container.project');
        var newProjectConfigA = $('#otp-project-config-hidden').clone().appendTo('.link-container.project-config');

        $.each([newProjectA, newProjectConfigA], function(index, newA) {
            newA.removeAttr('hidden');
            newA.removeAttr('id');
            newA.text(currentProject);
            newA.attr('href', newA.attr('href') + currentProject);
        });
        newProjectA.addClass('otp-links');
        newProjectConfigA.addClass('otp-project-config');
    }
}

function getMapFromSessionStorage(mapName) {
    var mapping = null;
    if (sessionStorage && mapName in sessionStorage) {
        var dump = sessionStorage.getItem(mapName);
        if (dump !== "{}") {
            mapping = new Map(JSON.parse(dump));
        }
    }
    return mapping;
}
