var active = 0;

$(document).ready(function() {
    $('td').click(function () {
        active = $('table td').index(this);
        selectNewCell();
    });

    $("form input").keypress(function (e) {
        if ((e.which && e.which == 13) || (e.keyCode && e.keyCode == 13)) {
            $(document.activeElement).blur();
            return false;
        } 
        return true;
    });
});

$(document).keydown(function (e) {
    if (e.keyCode == 9 || (e.keyCode > 36 && e.keyCode < 41)) {
        getNewCellIndex(e);
        selectNewCell();
    }
});

function getNewCellIndex(e) {
    var rows = $('.tblnavigate tbody tr').length;
    var columns = $('.tblnavigate tbody tr:eq(0) td').length;
    var temp;

    if (e.shiftKey && (e.keyCode == 9 || e.keyCode == 37)) { //move left with shift + tab
        e.preventDefault();
        temp = active;
        while (temp > 0) {
            temp = temp - 1;
            if ($('.tblnavigate tbody tr td').eq(temp).find('input[type=text]').length != 0) {
                active = temp;
                break;
            }
        }
    }
    if (e.shiftKey && e.keyCode == 38) { // move up
        temp = active;
        while (temp - columns >= 0) {
            temp = temp - columns;
            if ($('.tblnavigate tbody tr td').eq(temp).find('input[type=text]').length != 0) {
                active = temp;
                break;
            }
        }
    }
    if ((!e.shiftKey && e.keyCode == 9) || e.shiftKey && e.keyCode == 39) { // move right with tab
        e.preventDefault();
        temp = active;
        while (temp < (columns * rows) - 1) {
            temp = temp + 1;
            if ($('.tblnavigate tbody tr td').eq(temp).find('input[type=text]').length != 0) {
                active = temp;
                break;
            }
        }
    }
    if (e.shiftKey && e.keyCode == 40) { // move down
        temp = active;
        while (temp + columns <= (rows * columns) - 1) {
            temp = temp + columns;
            if ($('.tblnavigate tbody tr td').eq(temp).find('input[type=text]').length != 0) {
                active = temp;
                break;
            }
        }
    }
}

function selectNewCell() {
    // console.log(active);
    $('.active').removeClass('active');
    $('.tblnavigate tbody tr td').eq(active).addClass('active');
    // $('.tblnavigate tbody tr td').eq(active).find('input').select();
    $('.tblnavigate tbody tr td').eq(active).find('input').focus();

    // //scrollInView
    // var target = $('.tblnavigate tbody tr td:eq(' + active + ')');
    // if (target.length) {
    //     var top = target.offset().top;
    //
    //     $('html,body').stop().animate({
    //         scrollTop: top - 100
    //     }, 400);
    //     return false;
    // }
}
