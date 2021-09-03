package wec.workers;

public interface IThreadDoneListener<T> {
    void onThreadDone(T doneThread);
}
