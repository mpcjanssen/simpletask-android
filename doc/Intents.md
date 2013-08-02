### Intents

#### Introduction

Simpletask supports a couple of intents which can be used by other applications (e.g. tasker) to create tasks or display lists.

#### Create task in background

To create a task in the background, so without showing simpletask, you can use the intent:

* Intent action: `nl.mpcjanssen.simpletask.BACKGROUND_TASK`
* Intent string extra: `task`

the intent will have one extra string `task` which contains the task to be added.

For example to create a task from tasker use the following action:

* Action: nl.mpcjanssen.simpletask.BACKGROUND_TASK
* Cat: Default
* Mime Type: text/*
* Extra: task:\<Task text with possible variables here\> +tasker
* Target: Activity

I like to add the +tasker tag to be able to quickly filter tasks that were created by tasker.


