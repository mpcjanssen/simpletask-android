User Interface
==============

This page explains the user interface.  At the moment this is a work in
progress so it doesn't cover all items yet.


## Filter Activity

### Invert filter

The invert option is to use the negative of the selected items. To
give an example of why this is useful.

Imagine you have the following lists defined:

- `@work`
- `@home`
- `Project`
- `Someday`

Two of those lists contain next actions (`@work` and `@home`) the two
others don't.  Now if you want to create a filter to only show next
actions there are two ways to do this.  One wrong way and one right
way.

### The 'wrong' way

Create a filter with the `@work` and `@home` items checked.  Initially this
will work. However when you create an item with list `@shop` which is also
a next action, you will have to updateCache the next action filter.  So it
is easy to miss next actions like this.

### The 'right' way

Create a filter with the `Project` and `Someday` items checked and
also check `Invert filter`.  Now all items which are not on the
`Project` and `Someday` list will be shown after filtering.  This means
that if you add an item `@shop` this will also be included in the
results.

Of course if you add a new list which doesn't contain next actions
(e.g. `@PrivateProject`) this will also be shown and the filter still
needs to be changed.  The big difference with the 'wrong' way is that
you might get too much information instead of too little, which is preferable. 
