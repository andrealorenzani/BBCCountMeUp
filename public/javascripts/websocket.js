function openSocket(host, onMessage, onClose = function(){}, onOpen = function(){}){
    if(!("WebSocket" in window)){
        setTimeout(location.reload(true), 10000);
    }
    else{
        try{
            var socket = new WebSocket("ws://localhost:9000"+host);
            socket.onopen = onOpen;
            socket.onmessage = onMessage;
            socket.onclose = onClose;
            $( window ).unload(function() {
              socket.close();
            });
            return socket;
        } catch(exception){
            console.log("Failed to open connection: "+exception);
            setTimeout(location.reload(true), 10000);
        }
    }
};