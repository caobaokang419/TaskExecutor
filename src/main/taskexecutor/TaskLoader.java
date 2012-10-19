
package main.taskexecutor;

import main.taskexecutor.callbacks.ExecutorReferenceCallback;
import main.taskexecutor.core.Task;
import main.taskexecutor.core.TaskExecutor;
import main.taskexecutor.core.TaskExecutorService;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.Loader;

public abstract class TaskLoader<D> extends Loader<D> implements ExecutorReferenceCallback{
    
    private TaskExecutor mTaskExecutor  = null;
    private Handler	 mHandler 	= new Handler(Looper.getMainLooper());

    /**
     * Just like Tasks, define your asynchronous code needed to generate the data you want 
     * the Loader to manage. Return the data from the method you define.
     * @return
     * @throws Exception
     */
    protected abstract D loaderTask() throws Exception;
    
    public TaskLoader(Context context){
        super(context);
    }
    
    private final class LoaderTask extends Task{
	private D result = null;
	//Were not going to use the abstract task method
	//We override the run method so the Task doesn't call the internal post() method; there's 
	//reasoning to this that follows. We will not be using the Task's callback that is managed 
	//by the TaskExecutor. We want to use the Task and the TaskExecutor to consolidate all 
	//asynchronous Task execution, but we want the callback to be here specific to the Loader 
	//design pattern.
	@Override
	public void task() throws Exception {
	}
	@Override
	public void run(){
	    try{
		result = loaderTask();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    //No matter what post the results even if it's null.
	    postResults(result);
	}
    }

    private final void postResults(final D result){
	mHandler.post(new Runnable()
	{
	    @Override
	    public void run() {
		deliverResult(result);
	    }
	});
    }
    
    @Override
    protected void onStartLoading(){
	requestExecutorStartTask();
    }
    
    @Override
    protected void onForceLoad(){
	requestExecutorStartTask();
    }
    
    private final void requestExecutorStartTask(){
	TaskExecutorService.requestExecutorReference(TaskExecutorService.LOADER_IGNORE, this.getContext(), this, null);
    }

    @Override
    public void getTaskExecutorReference(TaskExecutor taskExecutor) {
	mTaskExecutor = taskExecutor;
	mTaskExecutor.executeTask(new LoaderTask());
    }
}