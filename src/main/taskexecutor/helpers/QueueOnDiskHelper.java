package main.taskexecutor.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Vector;

import main.taskexecutor.classes.Log;
import main.taskexecutor.core.Task;
import main.taskexecutor.core.Task.PersistenceObject;
import main.taskexecutor.core.TaskExecutor;
import android.content.Context;
import android.os.Parcel;

/**
 * @author Noah Seidman
 */
public class QueueOnDiskHelper {
    /**
     * @param context
     *            Provide a context.
     * @param taskExecutor
     *            Provide a reference to the TaskExecutor.
     * @return Returns true if Tasks were retrived from disk.
     * @throws Exception
     *             All the hoopla exceptions associated with reflection are
     *             thrown here and all stream related exceptions.
     */
    public static boolean retrieveTasksFromDisk(Context context, TaskExecutor taskExecutor) throws Exception {
	Vector<Task> tasks = getTasks(context, taskExecutor);
	if (tasks.size() > 0) {
	    taskExecutor.setQueue(tasks);
	    return true;
	}
	return false;
    }

    /**
     * @param Context
     * @param taskExecutor
     *            Provide a reference to the TaskExecutor.
     * @throws IOException
     */
    public static void updateTasksOnDisk(Context context, TaskExecutor taskExecutor) throws IOException {
	Vector<Task> localQueueCopy = new Vector<Task>(taskExecutor.getQueue());
	addFilesInQueue(localQueueCopy, context);
	deleteFilesNotIntQueue(localQueueCopy, context);
    }

    // ////////////////////////////////////////////////////
    // //////////Private methods hereforth/////////////////
    // ////////////////////////////////////////////////////

    private static Vector<Task> getTasks(Context context, TaskExecutor taskExecutor) throws Exception {
	Vector<Task> taskArray = new Vector<Task>();
	File[] tasks = getTaskExecutorFilesDir(context).listFiles();
	Log.d(QueueOnDiskHelper.class.getName(), "Number of Tasks being restored: " + tasks.length);
	for (File file : tasks) {
	    Parcel parcel = unmarshallParcel(file);
	    PersistenceObject persistenceObject = constructPersistenceObject(parcel);
	    Task task = inflateTask(persistenceObject, taskExecutor);
	    taskArray.add(task);
	}
	return taskArray;
    }

    private static Task inflateTask(PersistenceObject persistenceObject, TaskExecutor taskExecutor) throws Exception {
	String className = persistenceObject.getClassName();
	Class<?> clazzName = Class.forName(className);
	Constructor<?> constructor = clazzName.getConstructor();
	Task task = (Task) constructor.newInstance();
	task.setMainBundle(persistenceObject.getBundle());
	task.setTag(persistenceObject.getTag());
	task.setRemoveFromQueueOnException(persistenceObject.getRemoveFromQueueOnException());
	task.setRemoveFromQueueOnSuccess(persistenceObject.getRemoveFromQueueOnSuccess());
	task.setTaskExecutor(taskExecutor);
	Log.d(QueueOnDiskHelper.class.getName(), task.getTag() + " restored");
	return task;
    }

    private static PersistenceObject constructPersistenceObject(Parcel parcel) {
	PersistenceObject persistenceObject = PersistenceObject.CREATOR.createFromParcel(parcel);
	parcel.recycle();
	return persistenceObject;
    }

    private static Parcel unmarshallParcel(File file) throws Exception {
	Parcel parcel = Parcel.obtain();
	FileInputStream fIn = new FileInputStream(file);
	byte[] fileBytes = new byte[(int) file.length()];
	fIn.read(fileBytes);
	fIn.close();
	parcel.unmarshall(fileBytes, 0, fileBytes.length);
	parcel.setDataPosition(0);
	Log.d(QueueOnDiskHelper.class.getName(), "Data Available: " + parcel.dataAvail());
	Log.d(QueueOnDiskHelper.class.getName(), "Data Size: " + parcel.dataSize());
	return parcel;
    }

    private static void addFilesInQueue(Vector<Task> localQueueCopy, Context context) throws IOException {
	for (Task task : localQueueCopy) {
	    File taskFile = new File(getTaskExecutorFilesDir(context), task.getTag());
	    if (!taskFile.exists()) {
		FileOutputStream fos = new FileOutputStream(taskFile);
		PersistenceObject persistenceObject = new PersistenceObject(task.getClass().getName(), task.getMainBundle(), task.getTag(), task.getRemoveFromQueueOnSuccess(), task.getRemoveFromQueueOnException());
		Parcel parcel = Parcel.obtain();
		persistenceObject.writeToParcel(parcel, 0);
		try {
		    fos.write(parcel.marshall());
		    Log.d(QueueOnDiskHelper.class.getName(), task.getTag() + " written to disk");
		} finally {
		    fos.flush();
		    fos.close();
		    parcel.recycle();
		}
	    }
	}
    }

    private static void deleteFilesNotIntQueue(Vector<Task> queue, Context context) {
	File[] tasks = getTaskExecutorFilesDir(context).listFiles();
	for (int i = 0; i < tasks.length; i++) {
	    boolean delete = true;
	    for (Task task : queue) {
		// Tasks are written to disk eith the TAG being the name.
		if (task.getTag().equalsIgnoreCase(tasks[i].getName()))
		    delete = false;
	    }
	    if (delete) {
		Log.d(QueueOnDiskHelper.class.getName(), tasks[i].getName() + " deleted from disk");
		tasks[i].delete();
	    }
	}
    }

    private static File getTaskExecutorFilesDir(Context context) {
	File projDir = new File(context.getFilesDir().getAbsolutePath() + File.separator + "TaskExecutor");
	if (!projDir.exists())
	    projDir.mkdirs();
	return projDir;
    }
}
