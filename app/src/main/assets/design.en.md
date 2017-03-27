Design considerations
=====================

General design considerations
-----------------------------

-   Simple
    -   UI following Android UI guidelines
-   Enough functionallity to do GTD
    -   Seperate lists
    -   Ability to tag
-   Documented storage format
    -   todo.txt format with special handling of (.....)
-   Applicable for multiple usage scenarios
    -   Should be usable without dropbox

File format
-----------

### Lists (Contexts in todo.txt)

### Tags (Projects in todo.txt)

### Notes (No equivalent in todo.txt)

Other considerations
--------------------

-   Simpletask is written in such a way that the same documentation is used on Github and in the app. This requires some additional dependencies for the app (a Markdown and a HTML parser), but it makes keeping all the docs in sync much easier.

