/**
 * Created by Sofia on 2017-09-07.
 */
public class Step {

    private Pose Pose;
    private int Status;
    private int Timestamp;

    public Step(Pose pose, int status, int timestamp) {
        Pose = pose;
        Status = status;
        Timestamp = timestamp;
    }

    public Pose getPose() {
        return Pose;
    }

    public void setPose(Pose pose) {
        Pose = pose;
    }
}
