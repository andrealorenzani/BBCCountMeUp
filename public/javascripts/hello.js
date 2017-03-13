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
};


    function createTable(id, data) {
        $(id).alpaca({
        "data": data,
        "schema": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "label": {
                        "type": "string",
                        "title": "Candidate"
                    },
                    "y": {
                        "type": "number",
                        "title": "Votes"
                    }
                }
            }
        },
        "options": {
            "type": "table"
        },
        "view": "bootstrap-display"
    });
    }

window.onload = function() {
    if(this.pageFunction!==undefined){
        pageFunction();
    }
};