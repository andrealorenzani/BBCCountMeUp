if (window.console) {
  console.log("Welcome to your Play application's JavaScript!");
}

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
}

function sendData(url, data, func) {
    var res;
    $.ajax({
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
    return res;
}

function generateChart(container, type, event, votes) {
    var chart = new CanvasJS.Chart(container, {
        title:{
            text: event.name
        },
        data: [
        {
            // Change type to "doughnut", "line", "splineArea", etc.
            type: type,
            dataPoints: votes
        }
        ]
    });
    chart.render();
}

window.onload = function() {
    if(this.pageFunction!==undefined){
        pageFunction();
    }
};