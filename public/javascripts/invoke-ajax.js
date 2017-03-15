function invokeAjax(url, func) {
    return $.ajax({
            type: "POST", //rest Type
            dataType: 'json',
            url: url,
            async: true,
            contentType: "application/json; charset=utf-8",
            success: function (msg) {
                 if(func !== undefined) { func (msg) }
             }
    });
};

function sendData(url, data, func) {
    return $.ajax({
            type: "POST", //rest Type
            dataType: 'json',
            data: data,
            url: url,
            async: true,
            contentType: "application/json; charset=utf-8",
            success: function (msg) {
                if(func !== undefined) { func (msg) }
            }
    });
};

window.onload = function() {
    if(this.pageFunction!==undefined){
        pageFunction();
    }
};