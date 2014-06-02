package nl.mpcjanssen.simpletask;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.mpcjanssen.simpletask.task.LocalTaskRepository;
import nl.mpcjanssen.simpletask.task.Task;

/**
 * Created by janss484 on 1-6-2014.
 */
public class TestLocalTaskRepository implements LocalTaskRepository {
    @Override
    public void init() {

    }

    @Override
    public ArrayList<Task> load() {
        return null;
    }

    @Override
    public void store(ArrayList<Task> tasks) {

    }

    @Override
    public ArrayList<Task> archive(ArrayList<Task> tasks, List<Task> tasksToArchive) {
        return null;
    }

    @Override
    public boolean todoFileModifiedSince(Date date) {
        return false;
    }

    @Override
    public boolean doneFileModifiedSince(Date date) {
        return false;
    }

    @Override
    public File getDoneTxtFile() {
        return null;
    }

    @Override
    public File getTodoTxtFile() {
        return null;
    }

    @Override
    public void loadDoneTasks(File doneFile) {

    }

    @Override
    public void removeDoneFile() {

    }
}
