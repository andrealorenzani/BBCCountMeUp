function generateChart(container, type, event, votes) {
    if(event!==undefined) {
        window.storedevent=event;
    }
    else {
        event = window.storedevent;
    }

    if(votes!==undefined) {
        window.storedvotes=votes;
    }
    else {
        votes = window.storedvotes;
    }

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