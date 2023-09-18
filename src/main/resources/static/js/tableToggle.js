$(document).ready(function() {
    $(function () {
        $('.subTableCheckbox').each(function() {
            toggleSubmissionVisualization($(this));
        });
    });
});


function toggleSubmissionVisualization(checkbox) {
    let tableWrapper = $('#meta-table-' + checkbox.attr("id")).parent();
    if (tableWrapper != null) {
        if (checkbox.is( ":checked" )) {
            tableWrapper.show();
            executeAjax(tableWrapper.find("table").attr('id'));
        } else {
            tableWrapper.hide();
            tableWrapper.find(".active-row").remove();
            tableWrapper.find(".spinner-border").show();
        }
    }
}
