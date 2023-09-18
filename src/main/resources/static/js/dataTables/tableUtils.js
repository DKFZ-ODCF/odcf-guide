async function executeGenerateTableAjax(tableName, urlBasis) {
    let token = await getCookie('token').catch(() => {
        let spinner = document.getElementById(tableName).getElementsByClassName('spinner-border')[0];
        spinner.outerHTML = '<tr><td class="text-danger">Something went wrong. Please try to reload the page.</td></tr>';
    });
    let request = new XMLHttpRequest();
    request.open("GET", urlBasis + tableName, true);
    request.setRequestHeader("Content-type", "application/json");
    request.setRequestHeader('User-Token', token);
    request.onreadystatechange = function () {
        if (request.readyState === 4) {
            if (request.status === 200) {
                let data = JSON.parse(request.responseText);
                generateStatisticsTable(tableName, data);
            } else {
                let body = document.getElementById(tableName).getElementsByTagName('tbody')[0];
                let spinner = body.getElementsByClassName('spinner-border')[0];
                spinner.remove();
                let tr = document.createElement('tr');
                tr.classList.add("text-danger");
                tr.innerHTML = '<td>An error occurred while loading the data</td>';
                body.appendChild(tr);
            }
        }
    };
    request.send(null);
}
