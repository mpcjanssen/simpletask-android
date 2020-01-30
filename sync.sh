git fast-export --import-marks git.marks --export-marks git.marks --all | fossil import --incremental --git ../simpletask-android.fossil --export-marks fossil.marks --import-marks fossil.marks
