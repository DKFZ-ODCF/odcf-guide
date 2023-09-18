async function getContentForCurrentProject(currentProjectSelect, storageMapName, requestedField) {
    var project = currentProjectSelect.val();

    var requestedValue = '';
    var projectRequestedValueMapping = getMapFromSessionStorage(storageMapName);
    if (projectRequestedValueMapping != null) {
        requestedValue = projectRequestedValueMapping.get(project);
    } else {
        projectRequestedValueMapping = new Map();
    }

    if ((requestedValue === "" || requestedValue == null) && requestedValue !== "requestPending") {
        projectRequestedValueMapping.set(project, "requestPending");
        sessionStorage && (sessionStorage.setItem(storageMapName, JSON.stringify(Array.from(projectRequestedValueMapping.entries()))));

        return $.getJSON('/get-content-for-project', {
            projectName : project,
            requestedField : requestedField,
            ajax : 'true'
        }, function(contentFromRemote) {
            var callbackProjectRequestedValueMapping = getMapFromSessionStorage(storageMapName);
            callbackProjectRequestedValueMapping.set(project, contentFromRemote);
            sessionStorage && (sessionStorage.setItem(storageMapName, JSON.stringify(Array.from(callbackProjectRequestedValueMapping.entries()))));
            return contentFromRemote;
        });
    } 
    if (requestedValue === "requestPending") {
        while (requestedValue === "requestPending") {
                // wait here for ajax request to finish in order to provide cached value
            await Sleep(20);
            projectRequestedValueMapping = getMapFromSessionStorage(storageMapName);
            requestedValue = projectRequestedValueMapping.get(project);
        }
    }
    return requestedValue;
}
