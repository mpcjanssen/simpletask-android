Using the new Gradle build system
=================================

The build system has been moved from ant/Eclipse/Idea to gradle for two main reasons:

1.  Gradle makes it very easy to have different flavors of your app with the same code (e.g. a free and a donate one)
2.  Gradle is the new official Android build system so the change needs to happen anyway.

Build task
----------

Use `gradle(w) tasks` to get a full list, useful defined ones are:

-   `assembleReleaseFree`: build the free apk for google play
-   `assembleReleaseDonate`: build the donate apk
-   `installDevDebug`: build a version from current sources which can be installed in parallel with a google play version.
-   `connectedInstrumentTestUnitDebug`: As special flavor for unit testing. I don't want to use the Dev version for this as it will be uninstalled after the test run.
-   `check`: Depends on `connectedInstrumentTestUnitDebug` so it will run the test suite.
-   `build`: Builds all flavors of the app in one go, also depends on `check` so will run the test suite

