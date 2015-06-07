Manage Your Now
===============

Creating the contexts for MYN
-----------------------------

MYN defines 3 urgency zones, which we will map to lists in Simpletask in such a way that they sort correctly.

-   Critical Now -\> `@CriticalNow`
-   Opportunity Now -\> `@OpportunityNow`
-   Over The Horizon -\> `@OverTheHorizon`

Furthermore MYN defines a Significant Outcome (SOC) zone which needs to be at the top of you list. To achieve that, we map it to:

-   Significant Outcome -\> `@!SOC`

One disadvantage of the `todo.txt` format in combination with Simpletask is that if the file doesn't contain any items with a specific list, the list will not show up in Simpletask. To overcome this, Simpletask has a concept of hidden tasks. These tasked are marked with \`h:1' and by default will not show up in the task list. However any tags and lists defined on this task will be available.

So in order to make the MYN lists persistent, add a hidden task for each one of the e.g.:

    @CriticalNow h:1

Configuring the sort for MYN
----------------------------

![](./images/MYN_sort.png)

To defer tasks in Simpletask for Defer-To-Do or Defer-To-Review we use the threshold date functionallity. So make sure in settings the `Defer by threshold date` is checked. You can then use threshold date `t:yyyy-mm-dd` as startdate in MYN/1MTD

You can either hide future tasks (from the `Other` filter tab) or we can sort them to the end (so they are still visible but out of the way). To achieve that we use the `Threshold date in future` sort. The other main thing is to sort your list by reversed threshold date so that older tasks will be sorted lower on the list. Besides that it doesn't really matter how you sort after that, see picture the below for an example.

Using the list
--------------

When you are reviewing the items on your list and you want to defer one or more tasks, you can select them and use the `defer` menu item in the overflow menu. There are some prefilled options which you would use often in MYN/1MTD or you can defer to a specific date. After defering the task it will move to the bottom of the listview if it has been defered into the future and out of your face.

