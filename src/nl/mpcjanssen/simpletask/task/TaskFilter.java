package nl.mpcjanssen.simpletask.task;

/**
 * Created with IntelliJ IDEA.
 * User: A156712
 * Date: 22-1-13
 * Time: 12:25
 * To change this template use File | Settings | File Templates.
 */
public interface TaskFilter {
    public boolean apply(Task t);
}
