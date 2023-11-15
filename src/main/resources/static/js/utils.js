$(document).ready(async function() {
    $('[data-toggle="popover"]').popover();
    $('[data-toggle="tooltip"]').tooltip({trigger : 'hover'});

    let submissionIdentifier = document.getElementById('submissionIdentifier').value;
    let liveValidationEnabled = await getCookie('l_v_' + submissionIdentifier).catch(() => {});
    document.getElementById('liveValidation').checked = liveValidationEnabled === "true";

    var form = $('#metadataTableForm');
    if (form.length > 0) {
        validateMetadataTableForm(false);
        form[0].addEventListener('submit', function (event) {
            if (validateMetadataTableForm(true) === false) {
                event.preventDefault();
                event.stopPropagation();
                $('#client-val-popup').show();
            } else {
                $('#client-val-popup').hide();
            }
        }, false);
    }

    $('.changeable-sample-name').each(function() {
        changeSampleName($(this));
    });
});

function timer(timeout) {
    var countDownDate = (new Date().getTime() / 1000) + (timeout * 60);

    var x = setInterval(function () {
        var distance = countDownDate - (new Date().getTime() / 1000);
        var minutes = Math.floor((distance % (60 * 60)) / 60);
        var seconds = Math.floor((distance % 60));

        document.getElementById("timer").innerHTML = ('00' + minutes).slice(-2) + ":" + ('00' + seconds).slice(-2) + "";
        if (distance < 0) {
            var buttons = document.getElementsByName("button");
            for (let index = 0; index < buttons.length; index++) {
                buttons[index].disabled = true;
            }
            clearInterval(x);
            save_without_validation('Save on timeout');
        }
    }, 1000);

    return(x);
}

function set_timerInstance(ins){
    timerInstance = ins;
}

function reset_timer (timerInstance, timeout, UUID) {
    $.ajax({
        type: "POST",
        url: "/refresh-timer-in-db",
        data: "UUID="+UUID,
        dataType: 'text',
        cache: false,
        timeout: 600000,
        success: function (data) {
            console.log("TIMER RESET SUCCESS : ", data);
            clearInterval(timerInstance);
            timerInstance = timer(timeout);
            set_timerInstance(timerInstance);
        },
        error: function (e) {
            console.log("TIMER RESET ERROR : ", e);
        }
    });
}

function bulk_edit_dropdown(selector, value, text) {
    $('.otp-links').remove();
    $(selector + ':not([disabled])').each(function() {
        var option = $(this).find("option[value='"+ value +"']");
        if (option.length > 0) {
            $(this).find("option").removeAttr('selected');
            option.attr('selected', true);
            $(this).val(value);
            $(this).change();
        } else {
            $(this).append("<option value='" + value + "'>" + text + "</option>");
            $(this).val(value);
        }
    });
}

/**
 * Bulk edit function for multiple input dropdowns. Also handles loading spinner.
 * @param {string} selector fields to be edited
 * @param {object} origin bulk edit input
 */
function bulk_edit_dropdown_multi(selector, origin) {
    let spinner = origin.parentElement.getElementsByClassName('input-group-append')[0];
    spinner.style.display = 'flex';
    spinner.scrollIntoView({ block : "nearest", inline : "center", behavior: "smooth" });
    async_bulk_edit_dropdown_multi(selector, origin).then(function() {
        spinner.style.display = 'none';
    });
}

/**
 * Async part of bulk edit function for multiple input dropdowns to make it more user-friendly
 * @param {string} selector fields to be edited
 * @param {object} origin bulk edit input
 */
async function async_bulk_edit_dropdown_multi(selector, origin) {
    await Sleep(100);
    let dropdowns = origin.closest('table').querySelectorAll(`select.${selector}:not([disabled])`);
    for (let dropdown of dropdowns) {
        let dropdownSelectize = dropdown.selectize;
        dropdownSelectize.clear(origin.options.length > 0);
        for (let i = 0; i < origin.options.length; i++) {
            let value = origin.options[i].value;
            if (value.endsWith("(ReqVal)")) {
                dropdownSelectize.addOption({ value: value, text: value.replace("(ReqVal)", "") });
            }
            dropdownSelectize.addItem(value, i < origin.options.length - 1);
        }
    }
}

function bulk_edit_seqType(selector, element) {
    var seqTypeValue = element.value;
    var seqTypeText = element.selectedOptions[0].text;

    bulk_edit_dropdown(selector, seqTypeValue, seqTypeText);
}

function bulk_edit_checkbox(selector, value) {
    $(selector + ':not([disabled])').each(function() {
        $(this).prop( "checked", value);
        $(this).change();
    });
}

function bulk_edit_textfield(selector, value) {
    $(selector + ':not([disabled])').each(function() {
        $(this).prop('value', value);
        $(this).change();
    });
}

function submitByKeyboard(buttonId) {
    if(arguments.callee.caller.arguments[0].key === "Enter") {
        $('#'+buttonId).click();
    }
}

function checkPatternValid(checkInput) {
    var regex_pattern = checkInput.attr("pattern");
    var regex = RegExp(regex_pattern);

    if (regex.test(checkInput.val())) {
        checkInput.removeClass("is-invalid");
        return true;
    }
    checkInput.addClass("is-invalid");
    return false;
}

function openOrCloseContactForm() {
    var form = $('.nb-form');
    if(form.hasClass('active')) {
        form.removeClass('active');
    } else {
        form.addClass('active');
    }
}

function contactFormSendMail() {
    var is_valid = true;
    $('#contact-form input,textarea').each(function(){
        if ($(this).prop('required') && $(this).val().length === 0) {
            $(this).addClass('is-invalid');
            is_valid = false;
        } else {
            $(this).removeClass('is-invalid');
        }
    });

    if (!is_valid) {
        return false;
    }

    var name = $('#contactFormName').val();
    var mail = $('#contactFormMail').val();
    var message = $('#contactFormMessage').val();
    var submission = $('#submission').val();

    var form_data = "name=" + name + "&mail=" + mail + "&message=" + message;
    form_data += (submission !== null) ? "&submission=" + submission : "";

    $.ajax({
        type: "POST",
        url: "/send-mail",
        data: form_data,
        dataType: 'text',
        timeout: 60000,
        success: function () {
            $('#success').show();
            $('#form').hide();
            $('#send-button').hide();
            $('#contactFormMessage').val('');
        },
        error: function (e) {
            $('#error').show();
        }
    });
}

function loadContactForm() {
    $('#form').show();
    $('#send-button').show();
    $('#success').hide();
    $('#error').hide();
}

function feedbackFormSendMail() {
    if(!document.getElementById("feedbackFormMessage").validity.valid) {
        $('#feedbackFormMessage').popover({
            content: "Please provide a more detailed description of your user experience.",
            placement: 'bottom'
        });
        $('#feedbackFormMessage').popover('show');

    } else {
        var name = $('#feedbackFormName').val();
        var mail = $('#feedbackFormMail').val();
        var message = $('#feedbackFormMessage').val();
        var feedback = getCheckedFeedbackSmiley();
        var submission = $('#submission').val();

        var form_data = "name=" + name + "&mail=" + mail;
        form_data += (submission !== null) ? "&submission=" + submission : "";
        form_data += (message !== null) ? "&message=" + message : "";
        form_data += (feedback !== null) ? "&feedback=" + feedback : "";

        $.ajax({
            type: "POST",
            url: "/send-feedback",
            data: form_data,
            dataType: 'text',
            timeout: 60000,
            success: function () {
                $('#feedbackAlert').show();
                $('#feedbackModal').modal('toggle');
            },
            error: function (e) {
                $('#feedbackText').text('failed to send feedback');
                $('#feedbackAlert').addClass('alert-danger').show();
            }
        });
    }
}

function changeFeedbackTicketNumber(feedbackId, submission, ticket) {
    $('#changeFeedbackTicketNumberHeader').text(submission);
    $('#feedback').val(feedbackId);
    $('#feedbackTicketNumber').val(ticket);
}

function changeSubmissionTicketNumber(submission, ticket) {
    $('#changeSubmissionTicketNumberHeader').text(submission);
    $('#submission').val(submission);
    $('#submissionTicketNumber').val(ticket);
}

function changeSubmissionNameAndComment(submission, customName, submissionComment) {
    $('#changeSubmissionNameAndCommentHeader').text(submission);
    $('#submissionIdentifierForCustomisation').val(submission);
    $('#submissionCustomName').val(customName);
    $('#submissionCustomComment').val(submissionComment);
}

function changeSequencingDataReceived(submission, received) {
    $('#sequencingDataReceived').prop("checked", (received === 'true'));
    $('#sequencingDataReceivedSubmission').val(submission);
}

function changeSubmissionOnHold(submission, onHoldComment) {
    $('#submissionToToggleOnHold').val(submission);
    $('#stateComment').val(onHoldComment);
    $('#redirectToOverview').attr("checked", true);
}

function getCheckedFeedbackSmiley() {
    var ele = document.getElementsByName('smiley');

    for(i = 0; i < ele.length; i++) {
        if(ele[i].checked)
            return ele[i].value;
    }
}

function makeMessageRequiredForCheckedFeedbackSmiley(feedbackSmiley) {
    var feedback = feedbackSmiley.val();

    var textfieldToggle = $('#feedbackFormMessage');
    if (feedback === "happy") {
        textfieldToggle.attr('required', false);
    } else {
        textfieldToggle.attr('required', true);
    }
}

function toggleLiveValidation() {
    let liveValidationEnabled = document.getElementById('liveValidation').checked;
    let submissionIdentifier = document.getElementById('submissionIdentifier').value;
    setCookie('l_v_' + submissionIdentifier, liveValidationEnabled, 30);
    let form = document.getElementById('metadataTableForm');
    if (liveValidationEnabled && !form.classList.contains('was-validated')) {
        validateMetadataTableForm(false);
    } else if (!liveValidationEnabled) {
        let form = document.getElementById('metadataTableForm');
        form.classList.remove('was-validated');
    }
}

/**
 * Validate the metadata table
 * @returns {boolean} Validity of the form
 */
function validateMetadataTableForm(forceValidation) {
    let form = $('#metadataTableForm')[0];
    if (form.classList.contains('no-validation')) {
        return true;
    }
    if (!forceValidation && !document.getElementById('liveValidation').checked) {
        return false;
    }
    form.classList.add('was-validated');
    $(".stopped-sample").find('input, select')
        .removeAttr('pattern')
        .removeAttr('required')
        .prop( "disabled", true );
    $('.validation-custom .selectize-input').each(function() {
        let select = $(this).closest('td').find('select')[0];
        if (select.classList.contains('validation-needed')) {
            select.setCustomValidity(
                ($(this).attr('data-custom-valid') === 'false' || select.options.length === 0) ? 'invalid' : ''
            );
        } else if (select.classList.contains('validation-disabled')) {
            select.setCustomValidity('');
        }
    });
    let validity = form.checkValidity();
    $('.multiple-input-group').each(function() {
        $(this).toggleClass("flex-nowrap", $(this).find('input:invalid').length === 0);
    });
    return validity;
}

function changeFileInputLabel(fileInput) {
    var fileName = $('#tsvFileUpload')[0].files[0].name;
    //replace the "Choose a file" label
    fileInput.siblings('.custom-file-label').html(fileName);
}

function changeSampleName(element) {
    var row = element.parents('tr');
    var pid = row.find('.pid-textfield').val();
    var pidPrefix = row.find('.projectPrefix-span').text();
    var sampleType = row.find('.sampleType-textfield').val();
    var changeableSampleName = row.find('.changeable-sample-name');

    if (changeableSampleName.length === 1) {
        changeableSampleName.text(pidPrefix + pid + "_" + sampleType);
    }
}

function setCookie(cname, cvalue, exdays) {
    const d = new Date();
    d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
    let expires = "expires="+ d.toUTCString();
    document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function requestUserToken() {
    return new Promise(function (resolve) {
        if (!document.cookie.includes('token')) {
            let request = new XMLHttpRequest();
            request.open("GET", "/get-user-token", true);
            request.onreadystatechange = function () {
                if (request.readyState === 4 ) {
                    resolve('added');
                }
            };
            request.send(null);
        } else {
            resolve('found');
        }
    });
}

async function getCookie(cname) {
    await requestUserToken();
    return new Promise(function (resolve, reject) {
        let name = cname + "=";
        let ca = document.cookie.split(';');
        for (let i = 0; i < ca.length; i++) {
            let c = ca[i];
            while (c.charAt(0) == ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                resolve(c.substring(name.length, c.length));
            }
        }
        reject("unable to get cookie with name " + cname);
    });
}

Element.prototype.insertChildAtIndex = function(child, index) {
    if (!index) index = 0;
    let children = Array.from(this.children).filter(element => element.offsetParent !== null);
    if (index >= children.length) {
        this.appendChild(child);
    } else {
        this.insertBefore(child, children[index]);
    }
};

function hideNewNewsIndicator(){
    let indicator = document.getElementById("newsDot");
    if (indicator) {
        indicator.style.display = "none";
        const xhttp = new XMLHttpRequest();
        xhttp.open("POST", "/mark-news-as-read");
        xhttp.send();
    }
}

function copyTokenToClipboard() {
    let button = this.event.currentTarget;
    let apiToken = document.getElementById("apiToken");
    navigator.clipboard.writeText(apiToken.textContent).then(() => {
        button.classList.add("fa-check", "fa-solid");
        button.classList.remove("fa-clipboard", "fa-regular");
        setTimeout(function() {
            button.classList.remove("fa-check", "fa-solid");
            button.classList.add("fa-clipboard", "fa-regular");
            button.blur();
        }, 2000);
    });
}
