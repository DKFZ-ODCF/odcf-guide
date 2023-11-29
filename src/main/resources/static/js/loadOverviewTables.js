async function executeAjax(tableName) {
    let token = await getCookie('token').catch(() => {
        let spinner = document.getElementById(tableName).getElementsByClassName('spinner-border')[0];
        spinner.outerHTML = '<tr><td class="text-danger">Something went wrong. Please try to reload the page.</td></tr>';
    });
    let request = new XMLHttpRequest();
    request.open("GET", "get-submissions/" + tableName.replace('meta-table-', ''), true);
    request.setRequestHeader("Content-type", "application/json");
    request.setRequestHeader('User-Token', token);
    request.onreadystatechange = function () {
        if (request.readyState === 4 && request.status === 200) {
            let data = JSON.parse(request.responseText);
            generateTable(tableName, data);
        }
    };
    request.send(null);
}

function generateTable(tableName, data) {
    let table = document.getElementById(tableName);
    let body = table.getElementsByTagName('tbody')[0];
    let spinner = body.getElementsByClassName('spinner-border')[0];
    let submissionAdminLink = '/metadata-validator/submission/simple/admin?identifier=';
    let submissionUserLink = '/metadata-validator/submission/simple/user?uuid=';
    let ticketSystemBase = document.getElementById('ticketSystemBase')?.value;

    if (data.length === 0) {
        let tr = document.createElement('tr');
        tr.classList.add("active-row");
        tr.innerHTML = '<td>There are no submissions available</td>';
        body.appendChild(tr);
    }

    data.forEach(function(object) {
        let submissionLink;
        if (object.identifier != null) {
            submissionLink = submissionAdminLink + object.identifier;
        } else {
            submissionLink = submissionUserLink + object.uuid;
        }
        let tr = document.createElement('tr');
        tr.classList.add("active-row");
        tr.innerHTML = body.getElementsByClassName('blank_row')[0].innerHTML
            .replaceAll("SUBMISSION_LINK", submissionLink)
            .replaceAll("SUBMISSION_TEXT", object.submission)
            .replaceAll("SUBMISSION_ID", object.identifier)
            .replaceAll("CUSTOM_NAME", object.customName)
            .replaceAll("PROJECT", object.projectNames)
            .replaceAll("TICKET_URL", ticketSystemBase + object.ticketNumber)
            .replaceAll("TICKET_NUMBER", object.ticketNumber)
            .replaceAll("IMPORT_DATE", object.importDate)
            .replaceAll("RECEIVED", object.received)
            .replaceAll("EXTERNAL_DATA_AVAILABLE_FOR_MERGING", object.externalDataAvailableForMerging)
            .replaceAll("EDITOR", object.editor)
            .replaceAll("FINALLY", object.finally)
            .replaceAll("STATE", getStateIcon(object.state))
            .replaceAll("ON_HOLD_COMMENT", object.onHoldComment)
            .replaceAll("SUBMISSION_COMMENT", object.submissionComment)
            .replaceAll("CLUSTER_JOB", getJobIcon(object.clusterJob));
        if (object.stateNotActive === "true") {
            tr.getElementsByClassName("fa-pause")[0]?.remove();
        }
        if (object.state === "ON_HOLD") {
            let icon = tr.getElementsByClassName("fa-pause")[0];
            if (icon !== undefined) {
                icon.classList.remove("fa-pause");
                icon.classList.add("fa-play");
                icon.removeAttribute("data-toggle");
                icon.setAttribute("title", "Resume editing process");
                icon.setAttribute("onClick", "changeSubmissionOnHold('" + object.identifier + "');" +
                    "document.getElementById('changeOnHoldStateForm').submit();");
            }
        }
        body.appendChild(tr);
    });
    spinner.style.display = 'none';
}

function getStateIcon(state) {
    switch(state) {
        case 'IMPORTING':
            return '<i title="' + state + '" class="fa-solid fa-cloud-arrow-up"></i>';
        case 'IMPORTED':
            return '<i title="' + state + '" class="fas fa-file-import"></i>';
        case 'RESET':
            return '<i title="' + state + '" class="fas fa-clock-rotate-left"></i>';
        case 'EDITED':
            return '<i title="' + state + '" class="fas fa-user-edit"></i>';
        case 'VALIDATED':
            return '<i title="' + state + '" class="fas fa-clipboard-check"></i>';
        case 'CLOSED':
            return '<i title="' + state + '" class="fas fa-user-check"></i>';
        case 'TERMINATED':
            return '<i title="' + state + '" class="fas fa-ban"></i>';
        case 'AUTO_CLOSED':
            return '<i class="fas fa-robot"></i><i class="fas fa-thumbs-up"></i>';
        case 'FINISHED_EXTERNALLY':
            return '<i class="fas fa-person-walking-arrow-right"></i>';
        case 'EXPORTED':
            return '<i title="' + state + '" class="fas fa-file-export"></i>';
        case 'LOCKED':
            return '<i title="' + state + '" class="fas fa-lock"></i>';
        case 'UNLOCKED':
            return '<i title="' + state + '" class="fas fa-lock-open"></i>';
        case 'ON_HOLD':
            return '<i title="' + state + '" class="far fa-circle-pause"></i>';
    }
}

function getJobIcon(state) {
    switch(state) {
        case 'null':
            return '-';
        case 'SUBMITTED':
            return '<i title="' + state + '" class="fas fa-hourglass-start"></i>';
        case 'PENDING':
            return '<i title="' + state + '" class="fas fa-hourglass-half"></i>';
        case 'RUNNING':
            return '<i title="' + state + '" class="fas fa-gear fa-spin text-blue"></i>';
        case 'DONE':
            return '<i title="' + state + '" class="fas fa-check text-success"></i>';
        case 'FAILED':
            return '<i title="' + state + '" class="fas fa-xmark text-danger"></i>';
        case 'UNKNOWN':
            return '<i title="' + state + '" class="fas fa-question"></i>';
    }
}