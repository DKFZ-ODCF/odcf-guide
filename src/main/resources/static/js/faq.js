$(document).ready(function(){
    $('.toggle .toggle-title').click(function(){
        $(this).closest('.toggle').find('.toggle-inner').slideToggle("slow");
    });
    var id = window.location.href.split("#")[1];
    if (id !== null) {
        $("#"+id).find('.toggle-inner').slideToggle("slow");
    }
});