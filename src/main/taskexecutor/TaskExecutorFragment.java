package main.taskexecutor;

import main.taskexecutor.callbacks.ServiceCallbackDependentHelperCallback;
import main.taskexecutor.callbacks.TaskCompletedCallback;
import main.taskexecutor.callbacks.TaskExecutorReferenceCallback;
import android.support.v4.app.Fragment;

public abstract class TaskExecutorFragment extends Fragment implements
	ServiceCallbackDependentHelperCallback, TaskCompletedCallback,
	TaskExecutorReferenceCallback {

    protected TaskExecutor mTaskExecutor;

    /**
     * @return Task finess will pause currently running Tasks prior to their
     * hard callback to accommodate onPause events; this allows for the hard
     * callback to be reset when the activity is resumed. Be careful, what if
     * your activity isn't resumed for a long time? Block your Task's execution
     * wisely.
     */
    public abstract boolean allowTaskFiness();

    /**
     * Provide a mode, either CALLBACK_INCONSIDERATE, or CALLBACK_DEPENDENT.
     * This tells the service how to behave if it's restarted.
     * CALLBACK_DEPENDENT will not execute the queue and will wait for an
     * activity for a hard callback to be available. CALLBACK_INCONSIDERATE will
     * execute the queue without a hard callback being available.
     * 
     * @return return either TaskExecutorService.CALLBACK_INCONSIDERATE or
     * TaskExecutorService.CALLBACK_DEPENDENT.
     */
    public abstract int specifyServiceMode();

    /**
     * @return When the Service is in CALLBACK_DEPENDENT mode, and Tasks are
     * restored from a killed Service, should the queue auto execute on the next
     * Activity launch?
     */
    public abstract boolean autoExecuteAfterTasksRestored();

    @Override
    public void onPause() {
	super.onPause();
	// Theoretically the activity can be finishing before the request for
	// the executor reference is received.
	if (allowTaskFiness() && mTaskExecutor != null)
	    mTaskExecutor.restrainTasks();
    }

    @Override
    public void onResume() {
	super.onResume();
	TaskExecutorService.requestExecutorReference(specifyServiceMode(), this.getActivity(), this, this);
    }

    @Override
    public void getTaskExecutorReference(TaskExecutor taskExecutor) {
	mTaskExecutor = taskExecutor;
	if (allowTaskFiness())
	    mTaskExecutor.finessTasks(this);
    }

    @Override
    public void tasksHaveBeenRestored() {
	if (allowTaskFiness())
	    mTaskExecutor.finessTasks(this);
	if (autoExecuteAfterTasksRestored())
	    mTaskExecutor.executeQueue();
    }
}