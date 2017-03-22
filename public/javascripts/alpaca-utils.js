function createTable(id, data) {
        var tot = 0;
        for( var i = 0; i < data.length; i++ ) {
            tot = tot + data[i].y;
        }
        for( var i = 0; i < data.length; i++ ) {
            data[i].perc = (Math.floor((data[i].y*100)/tot))+"%";
        }

        if($(id).alpaca("exists")){
            $(id).alpaca("get").setValue(data);
        }
        else{

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
                                "type": "string",
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
        }
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
};

function generateVoterForm(where) {
    invokeAjax("/voter/event").then(function(eventret){
        var options = {};
        var names = [];
        for( var i = 0; i < eventret.candidates.length; i++ ) {
            options[eventret.candidates[i].name]= eventret.candidates[i].id;
            names[i] = eventret.candidates[i].name;
        }

        $(where).alpaca({
            "data": {
                "voter": "",
                "candidate": eventret.candidates[0].id,
                "eventid": eventret.id
            },
            "options": {

                "form": {
                    "attributes":{
                        "action": "/voter/addvote",
                        "method": "post"
                    },
                    "buttons": {
                        "submit": {
                            "click": function() {
                                var value = this.getValue();
                                value.candidate = options[value.candidate];
                                var data = JSON.stringify(value, null, "  ");
                                return sendData("/voter/addvote", data, function(msg){
                                    alert(msg.msg);
                                });
                            }
                        }
                    }
                },

                "fields":{
                    "voter":{
                        "placeholder": "Enter your voter id"
                    },
                    "candidate":{
                        "label": "Candidates",
                        "removeDefaultNone": true
                    },
                    "eventid": { "type": "hidden" }
                }
            },
            "schema": {
                "type": "object",
                "properties": {
                    "voter": {
                        "type": "string",
                        "title": "Voter"
                    },
                    "candidate": {
                        "type": "radio",
                        "title": "Select a candidate",
                        "required": true,
                        "enum": names
                    },
                    "eventid": {
                        "type": "number"
                    }
                }
            }
        });
    });
};