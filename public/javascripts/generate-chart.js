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