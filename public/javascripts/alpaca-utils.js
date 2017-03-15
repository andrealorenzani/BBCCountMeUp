function createTable(id, data) {
        var tot = 0;
        for( var i = 0; i < data.length; i++ ) {
            tot = tot + data[i].y;
        }
        for( var i = 0; i < data.length; i++ ) {
            data[i].perc = (Math.floor((data[i].y*100)/tot))+"%";
        }

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
                    },
                    "perc": {
                        "type": "number",
                        "title": "Votes (percentage)"
                    }
                }
            }
        },
        "options": {
            "type": "table"
        },
        "view": "bootstrap-display"
    });
};

function generateAdminForm(where) {
    $(where).alpaca({
        "schema": {
            "title": "Create new event",
            "type": "object",
            "properties": {
                "eventname": {
                    "type": "string",
                    "required": true,
                    "title": "Name of the event"
                },
                "description": {
                    "type": "string",
                    "required": true,
                    "title": "Description of the event"
                },
                "candidates": {
                    "type": "array",
                    "minItems": 2,
                    "items": {
                        "type": "table",
                        "properties": {
                            "name": {
                                "type": "string",
                                "required": true,
                                "title": "Name of the candidate"
                            },
                            "initialVotes": {
                                "type": "number",
                                "default": 0,
                                "title": "Initial Votes for the candidate"
                            }
                        }
                    }
                }
            }
        },
        "options": {
            "form": {
                "attributes":{
                    "action": "/admin/addevent",
                    "method": "post"
                },
                "buttons": {
                    "submit": {
                        "click": function() {
                            var value = this.getValue();
                            var data = JSON.stringify(value, null, "  ");
                            return sendData("/admin/addevent", data, function(msg){
                                alert('Event created!');
                            });
                        }
                    }
                }
            },
            "fields":{
                "eventname":{
                    "placeholder": "(Required) Enter a valid name for this event"
                },
                "description":{
                    "placeholder": "(Required) Enter a description of this event"
                },
                "candidates":{
                    "type":"table"
                }
            },
            "actionbar": {
                "actions": [{
                    "action": "add",
                    "enabled": false
                }]
            }
        }
    });
}